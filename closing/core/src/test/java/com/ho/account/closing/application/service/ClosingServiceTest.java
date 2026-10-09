package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.out.*;
import com.ho.account.closing.application.port.in.FinancialClosingCalculation;
import com.ho.account.closing.application.port.in.FinancialClosingCalculationResult;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private ClosingFinancialRunLockPort financialRunGate;
    @Mock
    private ClosingFinancialRunManifestPort financialRunManifest;
    @Mock
    private FiscalPeriodControlPort fiscalPeriodControlPort;
    @Mock
    private FinancialClosingCalculation financialClosingCalculation;
    @Mock
    private JournalQueryPort journalQueryPort;
    @Spy
    private ClosingAccountingProperties closingAccountingProperties = new ClosingAccountingProperties();

    @Mock
    private ClosingAggregatePersistencePort aggregates;
    @Mock
    private FinalCloseEvidenceService finalCloseEvidenceService;
    private ClosingService closingService;
    private ClosingAdmissionService closingAdmissionService;

    private FiscalPeriodRef openPeriod;
    private FiscalPeriodRef closedPeriod;

    @BeforeEach
    void setUp() {
        closingAdmissionService = new ClosingAdmissionService(
                fiscalPeriodControlPort, closingCalendarPersistencePort, periodLockPersistencePort);
        ClosingTransitionTransactions transactions = new ClosingTransitionTransactions(aggregates,
                closingCalendarPersistencePort, reopenApprovalPersistencePort, closingAuditLogPersistencePort,
                fiscalPeriodControlPort, finalCloseEvidenceService);
        ClosingPeriodTransitionService transitions = new ClosingPeriodTransitionService(transactions,
                closingCalendarPersistencePort);
        lenient().when(aggregates.lockCalendar(any(Long.class))).thenAnswer(call ->
                closingCalendarPersistencePort.findById(call.getArgument(0)));
        lenient().when(aggregates.lockCalendar(any(String.class), any(String.class))).thenAnswer(call ->
                closingCalendarPersistencePort.findByFiscalYearAndFiscalPeriod(call.getArgument(0), call.getArgument(1)));
        lenient().when(aggregates.findApprovalFiscalPeriodId(any())).thenAnswer(call ->
                reopenApprovalPersistencePort.findById(call.getArgument(0)).map(ReopenApproval::getFiscalPeriodId));
        lenient().when(aggregates.refreshApproval(any())).thenAnswer(call ->
                reopenApprovalPersistencePort.findById(call.getArgument(0)));
        lenient().when(aggregates.refreshTasks(any())).thenAnswer(call ->
                closingTaskPersistencePort.findByClosingCalendar(call.getArgument(0)));
        lenient().when(aggregates.refreshGates(any())).thenAnswer(call ->
                closingGatePersistencePort.findByClosingCalendar(call.getArgument(0)));
        closingService = new ClosingService(
                closingCalendarPersistencePort, closingTaskPersistencePort, closingGatePersistencePort,
                periodLockPersistencePort, reopenApprovalPersistencePort, batchExecutionRecorder,
                financialRunGate, financialRunManifest,
                closingAdjustmentPersistencePort, closingAuditLogPersistencePort,
                fiscalPeriodControlPort, journalQueryPort, closingAccountingProperties, financialClosingCalculation,
                closingAdmissionService, aggregates, transitions, transactions);
        lenient().when(financialRunGate.withExclusiveRun(any(), any(), any())).thenAnswer(call ->
                ((java.util.function.Supplier<?>) call.getArgument(2)).get());
        lenient().when(batchExecutionRecorder.findValuationByKey(any()))
                .thenReturn(valuationBatch(77L, ValuationBatch.ValuationBatchStatus.RUNNING));
        lenient().when(batchExecutionRecorder.findProvisionByKey(any()))
                .thenReturn(provisionBatch(88L, ProvisionBatch.ProvisionBatchStatus.RUNNING));
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
        when(aggregates.lockCalendar(any(String.class), any(String.class))).thenReturn(Optional.of(openCalendar()));
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
        when(aggregates.lockCalendar(any(String.class), any(String.class))).thenReturn(Optional.of(openCalendar()));
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
        when(aggregates.lockCalendar(any(String.class), any(String.class))).thenReturn(Optional.of(openCalendar()));
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
    void runValuationBatch_UsesFiscalEndDateAndPersistedBatchId() {
        when(fiscalPeriodControlPort.findFiscalPeriodById(1L)).thenReturn(Optional.of(openPeriod));
        when(batchExecutionRecorder.claimValuation(openPeriod, ValuationBatch.ValuationType.FX_RATE, "ADMIN", "key"))
                .thenReturn(valuationBatch(77L, ValuationBatch.ValuationBatchStatus.RUNNING));
        when(financialClosingCalculation.runFxValuation(openPeriod.endDate(), 77L))
                .thenReturn(new FinancialClosingCalculationResult(1, 900L));
        when(batchExecutionRecorder.finishValuation(77L, 1, 900L, false, "ADMIN"))
                .thenReturn(valuationBatch(77L, ValuationBatch.ValuationBatchStatus.PENDING_APPROVAL));

        ValuationBatch result = closingService.runValuationBatch(
                1L, ValuationBatch.ValuationType.FX_RATE, "ADMIN", "key");

        assertThat(result.getStatus()).isEqualTo(ValuationBatch.ValuationBatchStatus.PENDING_APPROVAL);
        verify(financialClosingCalculation).runFxValuation(LocalDate.of(2026, 1, 31), 77L);
        verify(batchExecutionRecorder).finishValuation(77L, 1, 900L, false, "ADMIN");
    }

    @ParameterizedTest
    @EnumSource(value = ValuationBatch.ValuationType.class, names = "FX_RATE", mode = EnumSource.Mode.EXCLUDE)
    void runValuationBatch_RejectsUnsupportedTypesBeforeHistory(ValuationBatch.ValuationType type) {
        assertThatThrownBy(() -> closingService.runValuationBatch(1L, type, "ADMIN", "key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only FX_RATE");

        verifyNoInteractions(fiscalPeriodControlPort, batchExecutionRecorder, financialClosingCalculation);
    }

    @Test
    void runValuationBatch_FxRateWithoutAuditedEvidence_RejectsBeforeJournalCreation() {
        when(fiscalPeriodControlPort.findFiscalPeriodById(1L)).thenReturn(Optional.of(openPeriod));
        when(batchExecutionRecorder.claimValuation(openPeriod, ValuationBatch.ValuationType.FX_RATE, "ADMIN", "key"))
                .thenReturn(valuationBatch(77L, ValuationBatch.ValuationBatchStatus.RUNNING));
        when(financialClosingCalculation.runFxValuation(openPeriod.endDate(), 77L))
                .thenThrow(new IllegalStateException("FX valuation source evidence is empty"));

        assertThatThrownBy(() -> closingService.runValuationBatch(
                1L, ValuationBatch.ValuationType.FX_RATE, "ADMIN", "key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FX valuation source evidence");

        verify(batchExecutionRecorder).valuationNoEffectFailure(77L, "ADMIN");
        verify(batchExecutionRecorder, never()).finishValuation(any(), anyInt(), any(), anyBoolean(), any());
    }

    @Test
    void runProvisionBatch_UsesFiscalEndDateAndPersistedBatchId() {
        when(fiscalPeriodControlPort.findFiscalPeriodById(1L)).thenReturn(Optional.of(openPeriod));
        when(batchExecutionRecorder.claimProvision(openPeriod, ProvisionBatch.ProvisionType.ECL, "ADMIN", "key"))
                .thenReturn(provisionBatch(88L, ProvisionBatch.ProvisionBatchStatus.RUNNING));
        when(financialClosingCalculation.runEclProvision(openPeriod.endDate(), 88L))
                .thenReturn(new FinancialClosingCalculationResult(1, 901L));
        when(batchExecutionRecorder.finishProvision(88L, 1, 901L, false, "ADMIN"))
                .thenReturn(provisionBatch(88L, ProvisionBatch.ProvisionBatchStatus.PENDING_APPROVAL));

        ProvisionBatch result = closingService.runProvisionBatch(
                1L, ProvisionBatch.ProvisionType.ECL, "ADMIN", "key");

        assertThat(result.getStatus()).isEqualTo(ProvisionBatch.ProvisionBatchStatus.PENDING_APPROVAL);
        verify(financialClosingCalculation).runEclProvision(LocalDate.of(2026, 1, 31), 88L);
        verify(batchExecutionRecorder).finishProvision(88L, 1, 901L, false, "ADMIN");
    }

    @ParameterizedTest
    @EnumSource(value = ProvisionBatch.ProvisionType.class, names = "ECL", mode = EnumSource.Mode.EXCLUDE)
    void runProvisionBatch_RejectsUnsupportedTypesBeforeHistory(ProvisionBatch.ProvisionType type) {
        assertThatThrownBy(() -> closingService.runProvisionBatch(1L, type, "ADMIN", "key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only ECL");

        verifyNoInteractions(fiscalPeriodControlPort, batchExecutionRecorder, financialClosingCalculation);
    }

    @Test
    void runProvisionBatch_EclWithoutFinalizedSummary_RejectsBeforeJournalCreation() {
        when(fiscalPeriodControlPort.findFiscalPeriodById(1L)).thenReturn(Optional.of(openPeriod));
        when(batchExecutionRecorder.claimProvision(openPeriod, ProvisionBatch.ProvisionType.ECL, "ADMIN", "key"))
                .thenReturn(provisionBatch(88L, ProvisionBatch.ProvisionBatchStatus.RUNNING));
        when(financialClosingCalculation.runEclProvision(openPeriod.endDate(), 88L))
                .thenThrow(new IllegalStateException("No finalized ECL allowance summary"));

        assertThatThrownBy(() -> closingService.runProvisionBatch(
                1L, ProvisionBatch.ProvisionType.ECL, "ADMIN", "key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No finalized ECL allowance summary");

        verify(batchExecutionRecorder).provisionNoEffectFailure(88L, "ADMIN");
        verify(batchExecutionRecorder, never()).finishProvision(any(), anyInt(), any(), anyBoolean(), any());
    }

    @Test
    void runValuationBatch_CalculationFailureRequiresReconciliationAndNoSuccessHistory() {
        when(financialRunManifest.exists("VALUATION", 77L)).thenReturn(true);
        when(fiscalPeriodControlPort.findFiscalPeriodById(1L)).thenReturn(Optional.of(openPeriod));
        when(batchExecutionRecorder.claimValuation(openPeriod, ValuationBatch.ValuationType.FX_RATE, "ADMIN", "key"))
                .thenReturn(valuationBatch(77L, ValuationBatch.ValuationBatchStatus.RUNNING));
        when(financialClosingCalculation.runFxValuation(openPeriod.endDate(), 77L))
                .thenThrow(new IllegalStateException("posting unavailable"));

        assertThatThrownBy(() -> closingService.runValuationBatch(
                1L, ValuationBatch.ValuationType.FX_RATE, "ADMIN", "key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("posting unavailable");

        verify(batchExecutionRecorder).valuationOutcomeUnknown(77L, null, "ADMIN");
        verify(batchExecutionRecorder, never()).finishValuation(any(), anyInt(), any(), anyBoolean(), any());
    }

    @Test
    void zeroAndManyJournalResultsDoNotInventHistoryJournalId() {
        when(fiscalPeriodControlPort.findFiscalPeriodById(1L)).thenReturn(Optional.of(openPeriod));
        when(batchExecutionRecorder.claimValuation(openPeriod, ValuationBatch.ValuationType.FX_RATE, "ADMIN", "key"))
                .thenReturn(valuationBatch(77L, ValuationBatch.ValuationBatchStatus.RUNNING));
        when(financialClosingCalculation.runFxValuation(openPeriod.endDate(), 77L))
                .thenReturn(new FinancialClosingCalculationResult(0, null));
        when(batchExecutionRecorder.finishValuation(77L, 0, null, false, "ADMIN"))
                .thenReturn(valuationBatch(77L, ValuationBatch.ValuationBatchStatus.COMPLETED));
        closingService.runValuationBatch(1L, ValuationBatch.ValuationType.FX_RATE, "ADMIN", "key");
        verify(batchExecutionRecorder).finishValuation(77L, 0, null, false, "ADMIN");

        when(batchExecutionRecorder.claimProvision(openPeriod, ProvisionBatch.ProvisionType.ECL, "ADMIN", "key"))
                .thenReturn(provisionBatch(88L, ProvisionBatch.ProvisionBatchStatus.RUNNING));
        when(financialClosingCalculation.runEclProvision(openPeriod.endDate(), 88L))
                .thenReturn(new FinancialClosingCalculationResult(2, null));
        when(batchExecutionRecorder.finishProvision(88L, 2, null, false, "ADMIN"))
                .thenReturn(provisionBatch(88L, ProvisionBatch.ProvisionBatchStatus.PENDING_APPROVAL));
        closingService.runProvisionBatch(1L, ProvisionBatch.ProvisionType.ECL, "ADMIN", "key");
        verify(batchExecutionRecorder).finishProvision(88L, 2, null, false, "ADMIN");
    }

    @Test
    void autoPostMarksSingleJournalResultCompletedWithItsId() {
        closingAccountingProperties.setAutoPostAdjustments(true);
        when(fiscalPeriodControlPort.findFiscalPeriodById(1L)).thenReturn(Optional.of(openPeriod));
        when(batchExecutionRecorder.claimProvision(openPeriod, ProvisionBatch.ProvisionType.ECL, "ADMIN", "key"))
                .thenReturn(provisionBatch(88L, ProvisionBatch.ProvisionBatchStatus.RUNNING));
        when(financialClosingCalculation.runEclProvision(openPeriod.endDate(), 88L))
                .thenReturn(new FinancialClosingCalculationResult(1, 901L));
        when(batchExecutionRecorder.finishProvision(88L, 1, 901L, true, "ADMIN"))
                .thenReturn(provisionBatch(88L, ProvisionBatch.ProvisionBatchStatus.COMPLETED));

        ProvisionBatch result = closingService.runProvisionBatch(
                1L, ProvisionBatch.ProvisionType.ECL, "ADMIN", "key");

        assertThat(result.getStatus()).isEqualTo(ProvisionBatch.ProvisionBatchStatus.COMPLETED);
        verify(batchExecutionRecorder).finishProvision(88L, 1, 901L, true, "ADMIN");
    }

    @Test
    void keyedRetryReturnsOriginalDraftWithoutReposting() {
        ValuationBatch original = valuationBatch(77L, ValuationBatch.ValuationBatchStatus.PENDING_APPROVAL);
        original.setGeneratedJournalEntryId(900L);
        when(fiscalPeriodControlPort.findFiscalPeriodById(1L)).thenReturn(Optional.of(openPeriod));
        when(batchExecutionRecorder.claimValuation(openPeriod, ValuationBatch.ValuationType.FX_RATE,
                "ADMIN", "key-1")).thenReturn(original);
        when(batchExecutionRecorder.findValuationByKey("key-1")).thenReturn(original);

        ValuationBatch retry = closingService.runValuationBatch(
                1L, ValuationBatch.ValuationType.FX_RATE, "ADMIN", "key-1");

        assertThat(retry).isSameAs(original);
        assertThat(retry.getGeneratedJournalEntryId()).isEqualTo(900L);
        verifyNoInteractions(financialClosingCalculation);
    }

    @Test
    void retryThatCannotAcquireKeyLockDoesNotChangeActiveRunEvidence() {
        ValuationBatch running = valuationBatch(77L, ValuationBatch.ValuationBatchStatus.RUNNING);
        when(fiscalPeriodControlPort.findFiscalPeriodById(1L)).thenReturn(Optional.of(openPeriod));
        when(batchExecutionRecorder.claimValuation(openPeriod, ValuationBatch.ValuationType.FX_RATE,
                "ADMIN", "busy-key")).thenReturn(running);
        doThrow(new org.springframework.dao.PessimisticLockingFailureException("key is busy") { })
                .when(financialRunGate).withExclusiveRun(eq("VALUATION"), eq("busy-key"), any());

        assertThatThrownBy(() -> closingService.runValuationBatch(
                1L, ValuationBatch.ValuationType.FX_RATE, "ADMIN", "busy-key"))
                .hasMessageContaining("key is busy");

        verify(batchExecutionRecorder, never()).valuationOutcomeUnknown(any(), any(), any());
        verify(batchExecutionRecorder, never()).valuationNoEffectFailure(any(), any());
        verifyNoInteractions(financialClosingCalculation);
    }

    @Test
    void gateCommitFailureAfterReadingOriginalSuccessDoesNotMarkItFailed() {
        ValuationBatch original = valuationBatch(77L, ValuationBatch.ValuationBatchStatus.PENDING_APPROVAL);
        when(fiscalPeriodControlPort.findFiscalPeriodById(1L)).thenReturn(Optional.of(openPeriod));
        when(batchExecutionRecorder.claimValuation(openPeriod, ValuationBatch.ValuationType.FX_RATE,
                "ADMIN", "finished-key")).thenReturn(original);
        when(batchExecutionRecorder.findValuationByKey("finished-key")).thenReturn(original);
        doAnswer(call -> {
                    ((java.util.function.Supplier<?>) call.getArgument(2)).get();
                    throw new IllegalStateException("gate commit failed");
                }).when(financialRunGate).withExclusiveRun(eq("VALUATION"), eq("finished-key"), any());

        assertThatThrownBy(() -> closingService.runValuationBatch(
                1L, ValuationBatch.ValuationType.FX_RATE, "ADMIN", "finished-key"))
                .hasMessageContaining("gate commit failed");
        verify(batchExecutionRecorder, never()).valuationNoEffectFailure(any(), any());
        verify(batchExecutionRecorder, never()).valuationOutcomeUnknown(any(), any(), any());
        verifyNoInteractions(financialClosingCalculation);
    }

    @Test
    void recorderFailureAfterDraftPreservesKnownEffectAndRetryReusesLineage() {
        when(financialRunManifest.exists("PROVISION", 88L)).thenReturn(true);
        ProvisionBatch running = provisionBatch(88L, ProvisionBatch.ProvisionBatchStatus.RUNNING);
        // Stateful fake Journal: the batch lineage owns one draft even if the recorder loses its reply.
        Map<Long, Long> journalByBatch = new HashMap<>();
        AtomicInteger draftsCreated = new AtomicInteger();
        when(fiscalPeriodControlPort.findFiscalPeriodById(1L)).thenReturn(Optional.of(openPeriod));
        when(batchExecutionRecorder.claimProvision(openPeriod, ProvisionBatch.ProvisionType.ECL,
                "ADMIN", "key-2"))
                .thenReturn(running);
        when(financialClosingCalculation.runEclProvision(openPeriod.endDate(), 88L))
                .thenAnswer(invocation -> new FinancialClosingCalculationResult(1,
                        journalByBatch.computeIfAbsent(invocation.getArgument(1), ignored -> {
                            draftsCreated.incrementAndGet();
                            return 901L;
                        })));
        when(batchExecutionRecorder.finishProvision(88L, 1, 901L, false, "ADMIN"))
                .thenThrow(new IllegalStateException("recorder unavailable"));

        assertThatThrownBy(() -> closingService.runProvisionBatch(
                1L, ProvisionBatch.ProvisionType.ECL, "ADMIN", "key-2"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("recorder unavailable");
        verify(batchExecutionRecorder).provisionOutcomeUnknown(88L, 1, "ADMIN");

        reset(batchExecutionRecorder);
        when(batchExecutionRecorder.findProvisionByKey("key-2")).thenReturn(running);
        when(batchExecutionRecorder.claimProvision(openPeriod, ProvisionBatch.ProvisionType.ECL,
                "ADMIN", "key-2"))
                .thenReturn(running);
        when(batchExecutionRecorder.finishProvision(88L, 1, 901L, false, "ADMIN"))
                .thenReturn(provisionBatch(88L, ProvisionBatch.ProvisionBatchStatus.PENDING_APPROVAL));
        ProvisionBatch recovered = closingService.runProvisionBatch(
                1L, ProvisionBatch.ProvisionType.ECL, "ADMIN", "key-2");
        assertThat(recovered.getId()).isEqualTo(88L);
        assertThat(draftsCreated).hasValue(1);
        verify(financialClosingCalculation, times(2)).runEclProvision(openPeriod.endDate(), 88L);
    }

    @Test
    void partialMultiDraftFailureLeavesUnknownCountForReconciliation() {
        when(financialRunManifest.exists("VALUATION", 77L)).thenReturn(true);
        ValuationBatch running = valuationBatch(77L, ValuationBatch.ValuationBatchStatus.RUNNING);
        when(fiscalPeriodControlPort.findFiscalPeriodById(1L)).thenReturn(Optional.of(openPeriod));
        when(batchExecutionRecorder.claimValuation(openPeriod, ValuationBatch.ValuationType.FX_RATE,
                "ADMIN", "key-3"))
                .thenReturn(running);
        when(financialClosingCalculation.runFxValuation(openPeriod.endDate(), 77L))
                .thenThrow(new IllegalStateException("second draft failed"));

        assertThatThrownBy(() -> closingService.runValuationBatch(
                1L, ValuationBatch.ValuationType.FX_RATE, "ADMIN", "key-3"))
                .hasMessageContaining("second draft failed");
        verify(batchExecutionRecorder).valuationOutcomeUnknown(77L, null, "ADMIN");
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
        FinalCloseEvidenceSet bound = validEvidence("unit-success", calendar, openPeriod);
        when(finalCloseEvidenceService.requireForFinalClose(calendar, openPeriod)).thenReturn(bound);
        when(fiscalPeriodControlPort.findFiscalPeriodById(openPeriod.id())).thenReturn(Optional.of(openPeriod));
        when(finalCloseEvidenceService.requireBoundForFinalClose(calendar, openPeriod, bound.evidenceSetId()))
                .thenReturn(bound);
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
        verify(finalCloseEvidenceService).requireBoundForFinalClose(calendar, openPeriod, bound.evidenceSetId());
    }

    @Test
    void completedConditionFreeChecklistCannotMutateTransitionAuditOrMasterWithoutTypedEvidence() {
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setId(10L);
        calendar.setFiscalYear("2026");
        calendar.setFiscalPeriod("01");
        calendar.setStatus(ClosingCalendar.ClosingCalendarStatus.IN_PROGRESS);
        ClosingTask task = new ClosingTask();
        task.setMandatory(true);
        task.setStatus(ClosingTask.ClosingTaskStatus.COMPLETED);
        ClosingGate gate = new ClosingGate();
        gate.setStatus(ClosingGate.ClosingGateStatus.PASSED);
        when(closingCalendarPersistencePort.findById(10L)).thenReturn(Optional.of(calendar));
        when(closingTaskPersistencePort.findByClosingCalendar(calendar)).thenReturn(List.of(task));
        when(closingGatePersistencePort.findByClosingCalendar(calendar)).thenReturn(List.of(gate));
        when(fiscalPeriodControlPort.findFiscalPeriod("2026", "01")).thenReturn(Optional.of(openPeriod));
        when(finalCloseEvidenceService.requireForFinalClose(calendar, openPeriod))
                .thenThrow(new FinalCloseEvidenceValidationException("no evidence set exists for the calendar"));

        assertThatThrownBy(() -> closingService.determineClosingStatus(10L, "ADMIN"))
                .isInstanceOf(FinalCloseEvidenceValidationException.class).hasMessageContaining("no evidence");

        assertThat(calendar.getTransitionId()).isNull();
        assertThat(calendar.getStatus()).isEqualTo(ClosingCalendar.ClosingCalendarStatus.IN_PROGRESS);
        verify(fiscalPeriodControlPort, never()).updateClosingStatus(any(), any(), any());
        verify(closingCalendarPersistencePort, never()).save(any());
        verify(closingAuditLogPersistencePort, never()).save(any());
    }

    private FinalCloseEvidenceSet validEvidence(String id, ClosingCalendar calendar, FiscalPeriodRef period) {
        return new FinalCloseEvidenceSet(id, calendar.getId(), period.id(), period.fiscalYear(), period.fiscalPeriod(),
                period.endDate(), java.time.Instant.parse("2026-02-01T00:00:00Z"), "provider",
                "a".repeat(64), List.of());
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
        when(finalCloseEvidenceService.requireForFinalClose(calendar, openPeriod))
                .thenReturn(validEvidence("admission-close", calendar, openPeriod));
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
        verifyNoInteractions(financialClosingCalculation, journalQueryPort);
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
        verifyNoInteractions(financialClosingCalculation);
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

    @Test
    void creationCannotMergeExistingEntitiesAroundTheAggregateLock() {
        ClosingCalendar calendar = openCalendar();
        ClosingTask task = new ClosingTask();
        task.setId(11L);
        task.setClosingCalendar(calendar);
        ClosingGate gate = new ClosingGate();
        gate.setId(12L);
        gate.setClosingCalendar(calendar);

        assertThatThrownBy(() -> closingService.createClosingCalendar(calendar))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("cannot overwrite");
        assertThatThrownBy(() -> closingService.createClosingTask(task))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("cannot overwrite");
        assertThatThrownBy(() -> closingService.createClosingGate(gate))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("cannot overwrite");
        verifyNoInteractions(fiscalPeriodControlPort, closingCalendarPersistencePort,
                closingTaskPersistencePort, closingGatePersistencePort, aggregates);
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
