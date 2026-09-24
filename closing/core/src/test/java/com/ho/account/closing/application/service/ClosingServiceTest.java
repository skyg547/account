package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.out.*;
import com.ho.account.closing.domain.*;
import com.ho.account.closing.infrastructure.external.ClosingStatusAdapter;
import com.ho.account.contracts.journal.*;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClosingServiceTest {

    @Mock
    private ClosingAdjustmentPersistencePort closingAdjustmentPersistencePort;
    @Mock
    private ClosingCalendarPersistencePort closingCalendarPersistencePort;
    @Mock
    private ClosingTaskPersistencePort closingTaskPersistencePort;
    @Mock
    private ClosingGatePersistencePort closingGatePersistencePort;
    @Mock
    private ClosingAuditLogPersistencePort closingAuditLogPersistencePort;
    @Mock
    private PeriodLockPersistencePort periodLockPersistencePort;
    @Mock
    private ReopenApprovalPersistencePort reopenApprovalPersistencePort;
    @Mock
    private ClosingBatchExecutionRecorder batchExecutionRecorder;
    @Mock
    private FiscalPeriodControlPort fiscalPeriodControlPort;
    @Mock
    private JournalPostingPort journalPostingPort;
    @Mock
    private JournalQueryPort journalQueryPort;
    @Spy
    private ClosingAccountingProperties closingAccountingProperties = new ClosingAccountingProperties();

    private ClosingService closingService;
    private ClosingAdmissionService closingAdmissionService;

    private FiscalPeriodRef openPeriod;
    private FiscalPeriodRef closedPeriod;

    @BeforeEach
    void setUp() {
        closingAdmissionService = new ClosingAdmissionService(
                fiscalPeriodControlPort, closingCalendarPersistencePort, periodLockPersistencePort);
        closingService = new ClosingService(
                closingCalendarPersistencePort, closingTaskPersistencePort, closingGatePersistencePort,
                periodLockPersistencePort, reopenApprovalPersistencePort, batchExecutionRecorder,
                closingAdjustmentPersistencePort, closingAuditLogPersistencePort,
                fiscalPeriodControlPort, journalPostingPort, journalQueryPort, closingAccountingProperties,
                closingAdmissionService);
        openPeriod = new FiscalPeriodRef(
                1L,
                "2026",
                "01",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                "OPEN");

        closedPeriod = new FiscalPeriodRef(
                2L,
                "2026",
                "02",
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 28),
                "CLOSED");
    }

    @Test
    @DisplayName("결산 캘린더 ID로 태스크 목록을 조회한다")
    void findClosingTasksByCalendarId_DelegatesToPersistencePort() {
        // given
        ClosingTask firstTask = new ClosingTask();
        firstTask.setId(101L);
        ClosingTask secondTask = new ClosingTask();
        secondTask.setId(102L);
        List<ClosingTask> tasks = List.of(firstTask, secondTask);
        when(closingTaskPersistencePort.findByClosingCalendarId(42L)).thenReturn(tasks);

        // when
        List<ClosingTask> result = closingService.findClosingTasksByCalendarId(42L);

        // then
        assertThat(result).containsExactly(firstTask, secondTask);
        verify(closingTaskPersistencePort).findByClosingCalendarId(42L);
    }

    @Test
    @DisplayName("결산 조정 전표 생성 시 회기가 OPEN이 아니면 예외가 발생한다")
    void createClosingAdjustment_PeriodNotOpen_ThrowsException() {
        // given
        when(fiscalPeriodControlPort.findFiscalPeriodById(2L)).thenReturn(Optional.of(closedPeriod));

        // when & then
        assertThatThrownBy(() -> closingService.createClosingAdjustment(2L, 100L, ClosingAdjustment.AdjustmentType.ACCRUAL, "Test", "ADMIN"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fiscal period is not OPEN");
    }

    @Test
    @DisplayName("결산 조정 전표 생성 시 전표가 대차 불일치면 예외가 발생한다")
    void createClosingAdjustment_UnbalancedJournal_ThrowsException() {
        // given
        when(fiscalPeriodControlPort.findFiscalPeriodById(1L)).thenReturn(Optional.of(openPeriod));
        
        JournalSummary summary = new JournalSummary();
        summary.setAccountingDate(LocalDate.of(2026, 1, 15));
        when(journalQueryPort.getJournalSummary(100L)).thenReturn(summary);

        JournalDetailSummary d1 = new JournalDetailSummary();
        d1.setSide(JournalSide.DEBIT);
        d1.setAmount(new BigDecimal("1000"));

        JournalDetailSummary d2 = new JournalDetailSummary();
        d2.setSide(JournalSide.CREDIT);
        d2.setAmount(new BigDecimal("900"));

        when(journalQueryPort.getJournalDetails(100L)).thenReturn(List.of(d1, d2));

        // when & then
        assertThatThrownBy(() -> closingService.createClosingAdjustment(1L, 100L, ClosingAdjustment.AdjustmentType.ACCRUAL, "Test", "ADMIN"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Journal is not balanced");
    }

    @Test
    @DisplayName("결산 조정 전표 생성 시 전표 회계 일자가 회기 범위를 벗어나면 예외가 발생한다")
    void createClosingAdjustment_AccountingDateOutsideRange_ThrowsException() {
        // given
        when(fiscalPeriodControlPort.findFiscalPeriodById(1L)).thenReturn(Optional.of(openPeriod));
        
        JournalSummary summary = new JournalSummary();
        summary.setAccountingDate(LocalDate.of(2026, 2, 1)); // 회기는 1월인데 전표는 2월
        when(journalQueryPort.getJournalSummary(100L)).thenReturn(summary);

        // when & then
        assertThatThrownBy(() -> closingService.createClosingAdjustment(1L, 100L, ClosingAdjustment.AdjustmentType.ACCRUAL, "Test", "ADMIN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Journal accounting date is outside the fiscal period range");
    }

    @Test
    @DisplayName("결산 조정은 FiscalPeriod 엔티티 참조 대신 회기 ID를 저장한다")
    void createClosingAdjustment_Success_StoresFiscalPeriodId() {
        // given
        when(fiscalPeriodControlPort.findFiscalPeriodById(1L)).thenReturn(Optional.of(openPeriod));

        JournalSummary summary = new JournalSummary();
        summary.setAccountingDate(LocalDate.of(2026, 1, 15));
        when(journalQueryPort.getJournalSummary(100L)).thenReturn(summary);

        JournalDetailSummary debit = new JournalDetailSummary();
        debit.setSide(JournalSide.DEBIT);
        debit.setAmount(new BigDecimal("1000"));

        JournalDetailSummary credit = new JournalDetailSummary();
        credit.setSide(JournalSide.CREDIT);
        credit.setAmount(new BigDecimal("1000"));

        when(journalQueryPort.getJournalDetails(100L)).thenReturn(List.of(debit, credit));
        when(closingAdjustmentPersistencePort.save(any(ClosingAdjustment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setFiscalYear("2026");
        calendar.setFiscalPeriod("01");
        when(closingCalendarPersistencePort.findByFiscalYearAndFiscalPeriod("2026", "01"))
                .thenReturn(Optional.of(calendar));

        // when
        ClosingAdjustment result = closingService.createClosingAdjustment(
                1L,
                100L,
                ClosingAdjustment.AdjustmentType.ACCRUAL,
                "Test",
                "ADMIN");

        // then
        assertThat(result.getFiscalPeriodId()).isEqualTo(1L);
        assertThat(result.getFiscalYear()).isEqualTo("2026");
        assertThat(result.getFiscalPeriod()).isEqualTo("01");
    }

    @Test
    @DisplayName("평가 배치는 설정된 계정과 금액으로 자동 분개를 생성한다")
    void runValuationBatch_UsesConfiguredAccountingRule() {
        // given
        closingAccountingProperties.setValuationRules(Map.of(
                ValuationBatch.ValuationType.FX_RATE,
                rule("510100", "110100", "1234.56")));
        when(fiscalPeriodControlPort.findFiscalPeriodById(1L)).thenReturn(Optional.of(openPeriod));
        ValuationBatch runningBatch = valuationBatch(77L, ValuationBatch.ValuationBatchStatus.RUNNING);
        ValuationBatch pendingBatch = valuationBatch(77L, ValuationBatch.ValuationBatchStatus.PENDING_APPROVAL);
        pendingBatch.setGeneratedJournalEntryId(900L);
        when(batchExecutionRecorder.startValuation(openPeriod, ValuationBatch.ValuationType.FX_RATE, "ADMIN"))
                .thenReturn(runningBatch);
        when(batchExecutionRecorder.markValuationPendingApproval(77L, 900L, "/reports/valuation/77", "ADMIN"))
                .thenReturn(pendingBatch);
        when(journalPostingPort.createDraftEntry(any(JournalEntryCommand.class)))
                .thenReturn(new JournalPostingResult(900L, "SLIP-900", "DRAFT"));

        // when
        ValuationBatch result = closingService.runValuationBatch(
                1L,
                ValuationBatch.ValuationType.FX_RATE,
                "ADMIN");

        // then
        assertThat(result.getStatus()).isEqualTo(ValuationBatch.ValuationBatchStatus.PENDING_APPROVAL);
        assertThat(result.getGeneratedJournalEntryId()).isEqualTo(900L);

        ArgumentCaptor<JournalEntryCommand> captor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(journalPostingPort).createDraftEntry(captor.capture());
        JournalEntryCommand command = captor.getValue();
        assertThat(command.lineageSourceType()).isEqualTo("VALUATION_BATCH");
        assertThat(command.lineageSourceId()).isEqualTo("77");
        assertThat(command.lines()).extracting(JournalLineCommand::accountCode)
                .containsExactly("510100", "110100");
        assertThat(command.lines()).allSatisfy(line ->
                assertThat(line.amount()).isEqualByComparingTo("1234.56"));
    }

    @Test
    @DisplayName("충당 배치는 설정된 계정과 금액으로 자동 분개를 생성한다")
    void runProvisionBatch_UsesConfiguredAccountingRule() {
        // given
        closingAccountingProperties.setProvisionRules(Map.of(
                ProvisionBatch.ProvisionType.BAD_DEBT,
                rule("550100", "129100", "789.10")));
        when(fiscalPeriodControlPort.findFiscalPeriodById(1L)).thenReturn(Optional.of(openPeriod));
        ProvisionBatch runningBatch = provisionBatch(88L, ProvisionBatch.ProvisionBatchStatus.RUNNING);
        ProvisionBatch pendingBatch = provisionBatch(88L, ProvisionBatch.ProvisionBatchStatus.PENDING_APPROVAL);
        pendingBatch.setGeneratedJournalEntryId(901L);
        when(batchExecutionRecorder.startProvision(openPeriod, ProvisionBatch.ProvisionType.BAD_DEBT, "ADMIN"))
                .thenReturn(runningBatch);
        when(batchExecutionRecorder.markProvisionPendingApproval(88L, 901L, "ADMIN"))
                .thenReturn(pendingBatch);
        when(journalPostingPort.createDraftEntry(any(JournalEntryCommand.class)))
                .thenReturn(new JournalPostingResult(901L, "SLIP-901", "DRAFT"));

        // when
        ProvisionBatch result = closingService.runProvisionBatch(
                1L,
                ProvisionBatch.ProvisionType.BAD_DEBT,
                "ADMIN");

        // then
        assertThat(result.getStatus()).isEqualTo(ProvisionBatch.ProvisionBatchStatus.PENDING_APPROVAL);
        assertThat(result.getGeneratedJournalEntryId()).isEqualTo(901L);

        ArgumentCaptor<JournalEntryCommand> captor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(journalPostingPort).createDraftEntry(captor.capture());
        JournalEntryCommand command = captor.getValue();
        assertThat(command.lineageSourceType()).isEqualTo("PROVISION_BATCH");
        assertThat(command.lineageSourceId()).isEqualTo("88");
        assertThat(command.lines()).extracting(JournalLineCommand::accountCode)
                .containsExactly("550100", "129100");
        assertThat(command.lines()).allSatisfy(line ->
                assertThat(line.amount()).isEqualByComparingTo("789.10"));
    }

    @Test
    @DisplayName("평가 전표 생성 실패 시 독립 이력에 FAILED를 기록한다")
    void runValuationBatch_PostingFails_RecordsFailure() {
        closingAccountingProperties.setValuationRules(Map.of(
                ValuationBatch.ValuationType.FX_RATE,
                rule("510100", "110100", "1234.56")));
        when(fiscalPeriodControlPort.findFiscalPeriodById(1L)).thenReturn(Optional.of(openPeriod));
        when(batchExecutionRecorder.startValuation(openPeriod, ValuationBatch.ValuationType.FX_RATE, "ADMIN"))
                .thenReturn(valuationBatch(77L, ValuationBatch.ValuationBatchStatus.RUNNING));
        when(journalPostingPort.createDraftEntry(any(JournalEntryCommand.class)))
                .thenThrow(new IllegalStateException("posting unavailable"));

        assertThatThrownBy(() -> closingService.runValuationBatch(
                1L,
                ValuationBatch.ValuationType.FX_RATE,
                "ADMIN"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Valuation batch failed")
                .hasCauseInstanceOf(IllegalStateException.class);

        verify(batchExecutionRecorder).markValuationFailed(77L, "ADMIN");
        verify(batchExecutionRecorder, never()).markValuationPendingApproval(any(), any(), any(), any());
    }

    @Test
    @DisplayName("평가 배치 회계 룰이 없으면 더미 분개를 생성하지 않고 실패한다")
    void runValuationBatch_MissingAccountingRule_ThrowsExceptionBeforePosting() {
        // given
        when(fiscalPeriodControlPort.findFiscalPeriodById(1L)).thenReturn(Optional.of(openPeriod));

        // when & then
        assertThatThrownBy(() -> closingService.runValuationBatch(
                1L,
                ValuationBatch.ValuationType.FX_RATE,
                "ADMIN"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("account.closing.accounting.valuation-rules.FX_RATE");

        verifyNoInteractions(batchExecutionRecorder, journalPostingPort);
    }

    @Test
    @DisplayName("필수 태스크가 완료되지 않았으면 결산을 완료할 수 없다")
    void determineClosingStatus_MandatoryTaskNotCompleted_ThrowsException() {
        // given
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setId(10L);
        when(closingCalendarPersistencePort.findById(10L)).thenReturn(Optional.of(calendar));

        ClosingTask mandatoryTask = new ClosingTask();
        mandatoryTask.setMandatory(true);
        mandatoryTask.setStatus(ClosingTask.ClosingTaskStatus.IN_PROGRESS);

        when(closingTaskPersistencePort.findByClosingCalendar(calendar)).thenReturn(List.of(mandatoryTask));

        // when & then
        assertThatThrownBy(() -> closingService.determineClosingStatus(10L, "ADMIN"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not all mandatory tasks are completed");
    }

    @Test
    @DisplayName("게이트를 통과하지 못했으면 결산을 완료할 수 없다")
    void determineClosingStatus_GateNotPassed_ThrowsException() {
        // given
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setId(10L);
        when(closingCalendarPersistencePort.findById(10L)).thenReturn(Optional.of(calendar));

        ClosingTask mandatoryTask = new ClosingTask();
        mandatoryTask.setMandatory(true);
        mandatoryTask.setStatus(ClosingTask.ClosingTaskStatus.COMPLETED);
        when(closingTaskPersistencePort.findByClosingCalendar(calendar)).thenReturn(List.of(mandatoryTask));

        ClosingGate gate = new ClosingGate();
        gate.setStatus(ClosingGate.ClosingGateStatus.PENDING);
        when(closingGatePersistencePort.findByClosingCalendar(calendar)).thenReturn(List.of(gate));

        // when & then
        assertThatThrownBy(() -> closingService.determineClosingStatus(10L, "ADMIN"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not all closing gates are passed");
    }

    @Test
    @DisplayName("모든 조건 충족 시 결산이 정상적으로 완료된다")
    void determineClosingStatus_Success() {
        // given
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setId(10L);
        calendar.setFiscalYear("2026");
        calendar.setFiscalPeriod("01");
        calendar.setStatus(ClosingCalendar.ClosingCalendarStatus.IN_PROGRESS);
        when(closingCalendarPersistencePort.findById(10L)).thenReturn(Optional.of(calendar));

        ClosingTask mandatoryTask = new ClosingTask();
        mandatoryTask.setMandatory(true);
        mandatoryTask.setStatus(ClosingTask.ClosingTaskStatus.COMPLETED);
        when(closingTaskPersistencePort.findByClosingCalendar(calendar)).thenReturn(List.of(mandatoryTask));

        ClosingGate gate = new ClosingGate();
        gate.setStatus(ClosingGate.ClosingGateStatus.PASSED);
        when(closingGatePersistencePort.findByClosingCalendar(calendar)).thenReturn(List.of(gate));

        when(fiscalPeriodControlPort.findFiscalPeriod("2026", "01")).thenReturn(Optional.of(openPeriod));
        when(fiscalPeriodControlPort.updateClosingStatus(1L, "CLOSED", "ADMIN")).thenReturn(
                new FiscalPeriodRef(
                        1L,
                        "2026",
                        "01",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31),
                        "CLOSED"));
        when(closingCalendarPersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        ClosingCalendar result = closingService.determineClosingStatus(10L, "ADMIN");

        // then
        assertThat(result.getStatus()).isEqualTo(ClosingCalendar.ClosingCalendarStatus.CLOSED);
        assertThat(result.getClosedBy()).isEqualTo("ADMIN");
        verify(fiscalPeriodControlPort).updateClosingStatus(1L, "CLOSED", "ADMIN");
    }

    @Test
    @DisplayName("회계기간 기준정보가 없으면 마감 여부를 OPEN으로 추정하지 않는다")
    void isClosed_MissingFiscalPeriod_FailsClosed() {
        LocalDate accountingDate = LocalDate.of(2026, 3, 15);
        when(fiscalPeriodControlPort.findFiscalPeriod("2026", "03")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> closingService.isClosed(accountingDate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fiscal period is missing");
    }

    @ParameterizedTest
    @EnumSource(PeriodLock.PeriodLockType.class)
    void isClosed_ActiveLockBlocksOrdinaryJournal(PeriodLock.PeriodLockType lockType) {
        LocalDate accountingDate = LocalDate.of(2026, 1, 15);
        when(fiscalPeriodControlPort.findFiscalPeriod("2026", "01")).thenReturn(Optional.of(openPeriod));
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setFiscalYear("2026");
        calendar.setFiscalPeriod("01");
        calendar.setStatus(ClosingCalendar.ClosingCalendarStatus.OPEN);
        when(closingCalendarPersistencePort.findByFiscalYearAndFiscalPeriod("2026", "01"))
                .thenReturn(Optional.of(calendar));
        PeriodLock lock = new PeriodLock();
        lock.assignFiscalPeriod(1L, "2026", "01");
        lock.setLockType(lockType);
        when(periodLockPersistencePort.findByFiscalPeriodId(1L)).thenReturn(Optional.of(lock));

        assertThat(closingService.isClosed(accountingDate)).isTrue();
        assertThat(new ClosingStatusAdapter(closingAdmissionService).isClosed(accountingDate)).isTrue();
    }

    @Test
    void isClosed_InProgressCalendarBlocksOrdinaryJournal() {
        LocalDate accountingDate = LocalDate.of(2026, 1, 15);
        when(fiscalPeriodControlPort.findFiscalPeriod("2026", "01")).thenReturn(Optional.of(openPeriod));
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setFiscalYear("2026");
        calendar.setFiscalPeriod("01");
        calendar.setStatus(ClosingCalendar.ClosingCalendarStatus.IN_PROGRESS);
        when(closingCalendarPersistencePort.findByFiscalYearAndFiscalPeriod("2026", "01"))
                .thenReturn(Optional.of(calendar));

        assertThat(closingService.isClosed(accountingDate)).isTrue();
        assertThat(new ClosingStatusAdapter(closingAdmissionService).isClosed(accountingDate)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(PeriodLock.PeriodLockType.class)
    void ordinaryAdmissionTracksLockAndUnlockWithoutClosingMaster(PeriodLock.PeriodLockType lockType) {
        LocalDate accountingDate = LocalDate.of(2026, 1, 15);
        ClosingCalendar calendar = openCalendar();
        AtomicReference<PeriodLock> activeLock = new AtomicReference<>();
        when(fiscalPeriodControlPort.findFiscalPeriod("2026", "01")).thenReturn(Optional.of(openPeriod));
        when(fiscalPeriodControlPort.findFiscalPeriodById(1L)).thenReturn(Optional.of(openPeriod));
        when(closingCalendarPersistencePort.findByFiscalYearAndFiscalPeriod("2026", "01"))
                .thenReturn(Optional.of(calendar));
        trackActiveLock(activeLock);
        ClosingStatusAdapter adapter = new ClosingStatusAdapter(closingAdmissionService);

        assertThat(adapter.isClosed(accountingDate)).isFalse();
        closingService.lockPeriod(1L, lockType, "CLOSER", "Prepare closing");
        assertThat(adapter.isClosed(accountingDate)).isTrue();
        closingService.unlockPeriod(1L, "CLOSER");
        assertThat(adapter.isClosed(accountingDate)).isFalse();

        assertThat(calendar.getStatus()).isEqualTo(ClosingCalendar.ClosingCalendarStatus.OPEN);
        verify(fiscalPeriodControlPort, never()).updateClosingStatus(any(), any(), any());
        ArgumentCaptor<ClosingAuditLog> audit = ArgumentCaptor.forClass(ClosingAuditLog.class);
        verify(closingAuditLogPersistencePort, times(2)).save(audit.capture());
        assertThat(audit.getAllValues()).extracting(ClosingAuditLog::getActionType)
                .containsExactly(ClosingAuditLog.ActionType.PERIOD_LOCK, ClosingAuditLog.ActionType.PERIOD_UNLOCK);
    }

    @Test
    void startCloseAndReopenRequireEveryIndependentAdmissionControlToOpen() {
        LocalDate accountingDate = LocalDate.of(2026, 1, 15);
        ClosingCalendar calendar = openCalendar();
        AtomicReference<FiscalPeriodRef> master = new AtomicReference<>(openPeriod);
        AtomicReference<PeriodLock> activeLock = new AtomicReference<>();
        AtomicReference<ReopenApproval> reopenRequest = new AtomicReference<>();
        when(fiscalPeriodControlPort.findFiscalPeriod("2026", "01"))
                .thenAnswer(invocation -> Optional.of(master.get()));
        when(fiscalPeriodControlPort.findFiscalPeriodById(1L))
                .thenAnswer(invocation -> Optional.of(master.get()));
        when(fiscalPeriodControlPort.updateClosingStatus(eq(1L), any(), any())).thenAnswer(invocation -> {
            FiscalPeriodRef updated = new FiscalPeriodRef(
                    1L, "2026", "01", openPeriod.startDate(), openPeriod.endDate(), invocation.getArgument(1));
            master.set(updated);
            return updated;
        });
        when(closingCalendarPersistencePort.findByFiscalYearAndFiscalPeriod("2026", "01"))
                .thenReturn(Optional.of(calendar));
        when(closingCalendarPersistencePort.findById(10L)).thenReturn(Optional.of(calendar));
        when(closingCalendarPersistencePort.save(calendar)).thenReturn(calendar);
        trackActiveLock(activeLock);
        ClosingTask task = new ClosingTask();
        task.setMandatory(true);
        task.setStatus(ClosingTask.ClosingTaskStatus.COMPLETED);
        ClosingGate gate = new ClosingGate();
        gate.setStatus(ClosingGate.ClosingGateStatus.PASSED);
        when(closingTaskPersistencePort.findByClosingCalendar(calendar)).thenReturn(List.of(task));
        when(closingGatePersistencePort.findByClosingCalendar(calendar)).thenReturn(List.of(gate));
        when(reopenApprovalPersistencePort.save(any())).thenAnswer(invocation -> {
            ReopenApproval approval = invocation.getArgument(0);
            approval.setId(20L);
            reopenRequest.set(approval);
            return approval;
        });
        when(reopenApprovalPersistencePort.findById(20L))
                .thenAnswer(invocation -> Optional.of(reopenRequest.get()));
        ClosingStatusAdapter adapter = new ClosingStatusAdapter(closingAdmissionService);

        assertThat(adapter.isClosed(accountingDate)).isFalse();
        closingService.updateClosingCalendarStatus(10L, ClosingCalendar.ClosingCalendarStatus.IN_PROGRESS, "CLOSER");
        assertThat(adapter.isClosed(accountingDate)).isTrue();
        closingService.lockPeriod(1L, PeriodLock.PeriodLockType.NON_ADJUSTMENT_ENTRIES, "CLOSER", "Prepare closing");
        closingService.unlockPeriod(1L, "CLOSER");
        assertThat(adapter.isClosed(accountingDate)).isTrue();
        assertThat(master.get().closingStatus()).isEqualTo("OPEN");

        closingService.determineClosingStatus(10L, "CLOSER");
        assertThat(master.get().closingStatus()).isEqualTo("CLOSED");
        assertThat(adapter.isClosed(accountingDate)).isTrue();
        closingService.lockPeriod(1L, PeriodLock.PeriodLockType.ALL_TRANSACTIONS, "CLOSER", "Hold during reopen");
        closingService.requestPeriodReopen(1L, "REQUESTER", "Approved correction required");
        assertThat(adapter.isClosed(accountingDate)).isTrue();
        closingService.updateReopenApprovalStatus(20L, ReopenApproval.ReopenApprovalStatus.APPROVED, "APPROVER");
        assertThat(master.get().closingStatus()).isEqualTo("OPEN");
        assertThat(calendar.getStatus()).isEqualTo(ClosingCalendar.ClosingCalendarStatus.OPEN);
        // Reopen approves the calendar/master transition; it cannot release an independently held lock.
        assertThat(adapter.isClosed(accountingDate)).isTrue();
        closingService.unlockPeriod(1L, "APPROVER");
        assertThat(adapter.isClosed(accountingDate)).isFalse();

        verify(fiscalPeriodControlPort).updateClosingStatus(1L, "CLOSED", "CLOSER");
        verify(fiscalPeriodControlPort).updateClosingStatus(1L, "OPEN", "APPROVER");
        verifyNoInteractions(journalPostingPort, journalQueryPort);
    }

    @Test
    void blockedOrdinaryAdmissionPreservesExistingControlledAdjustmentRegistration() {
        LocalDate accountingDate = LocalDate.of(2026, 1, 15);
        ClosingCalendar calendar = openCalendar();
        calendar.start("CLOSER");
        when(fiscalPeriodControlPort.findFiscalPeriod("2026", "01")).thenReturn(Optional.of(openPeriod));
        when(fiscalPeriodControlPort.findFiscalPeriodById(1L)).thenReturn(Optional.of(openPeriod));
        when(closingCalendarPersistencePort.findByFiscalYearAndFiscalPeriod("2026", "01"))
                .thenReturn(Optional.of(calendar));
        JournalSummary summary = new JournalSummary();
        summary.setAccountingDate(accountingDate);
        when(journalQueryPort.getJournalSummary(100L)).thenReturn(summary);
        JournalDetailSummary debit = new JournalDetailSummary();
        debit.setSide(JournalSide.DEBIT);
        debit.setAmount(new BigDecimal("1000"));
        JournalDetailSummary credit = new JournalDetailSummary();
        credit.setSide(JournalSide.CREDIT);
        credit.setAmount(new BigDecimal("1000"));
        when(journalQueryPort.getJournalDetails(100L)).thenReturn(List.of(debit, credit));
        when(closingAdjustmentPersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(new ClosingStatusAdapter(closingAdmissionService).isClosed(accountingDate)).isTrue();
        ClosingAdjustment adjustment = closingService.createClosingAdjustment(
                1L, 100L, ClosingAdjustment.AdjustmentType.ACCRUAL, "Controlled correction", "APPROVER");

        assertThat(adjustment.getJournalEntryId()).isEqualTo(100L);
        assertThat(adjustment.getApprovedBy()).isEqualTo("APPROVER");
        assertThat(calendar.getStatus()).isEqualTo(ClosingCalendar.ClosingCalendarStatus.IN_PROGRESS);
        verify(fiscalPeriodControlPort, never()).updateClosingStatus(any(), any(), any());
        verifyNoInteractions(journalPostingPort);
    }

    @Test
    @DisplayName("캘린더 상태는 OPEN으로 직접 되돌릴 수 없다")
    void updateClosingCalendarStatus_DirectOpenRejected() {
        assertThatThrownBy(() -> closingService.updateClosingCalendarStatus(
                10L,
                ClosingCalendar.ClosingCalendarStatus.OPEN,
                "ADMIN"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("controlled flows");
    }

    private ClosingCalendar openCalendar() {
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setId(10L);
        calendar.setFiscalYear("2026");
        calendar.setFiscalPeriod("01");
        calendar.setStatus(ClosingCalendar.ClosingCalendarStatus.OPEN);
        return calendar;
    }

    private void trackActiveLock(AtomicReference<PeriodLock> activeLock) {
        when(periodLockPersistencePort.findByFiscalPeriodId(1L))
                .thenAnswer(invocation -> Optional.ofNullable(activeLock.get()));
        when(periodLockPersistencePort.save(any())).thenAnswer(invocation -> {
            PeriodLock saved = invocation.getArgument(0);
            activeLock.set(saved);
            return saved;
        });
        doAnswer(invocation -> {
            activeLock.set(null);
            return null;
        }).when(periodLockPersistencePort).delete(any());
    }

    private ClosingAccountingProperties.AutomatedJournalRule rule(
            String debitAccountCode,
            String creditAccountCode,
            String amount) {
        ClosingAccountingProperties.AutomatedJournalRule rule = new ClosingAccountingProperties.AutomatedJournalRule();
        rule.setDebitAccountCode(debitAccountCode);
        rule.setCreditAccountCode(creditAccountCode);
        rule.setAmount(new BigDecimal(amount));
        return rule;
    }

    private ValuationBatch valuationBatch(
            Long id,
            ValuationBatch.ValuationBatchStatus status) {
        ValuationBatch batch = new ValuationBatch();
        batch.setId(id);
        batch.setStatus(status);
        return batch;
    }

    private ProvisionBatch provisionBatch(
            Long id,
            ProvisionBatch.ProvisionBatchStatus status) {
        ProvisionBatch batch = new ProvisionBatch();
        batch.setId(id);
        batch.setStatus(status);
        return batch;
    }
}
