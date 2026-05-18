package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.out.*;
import com.ho.account.closing.domain.*;
import com.ho.account.contracts.journal.*;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private ValuationBatchPersistencePort valuationBatchPersistencePort;
    @Mock
    private ProvisionBatchPersistencePort provisionBatchPersistencePort;
    @Mock
    private FiscalPeriodControlPort fiscalPeriodControlPort;
    @Mock
    private JournalPostingPort journalPostingPort;
    @Mock
    private JournalQueryPort journalQueryPort;
    @Spy
    private ClosingAccountingProperties closingAccountingProperties = new ClosingAccountingProperties();

    @InjectMocks
    private ClosingService closingService;

    private FiscalPeriodRef openPeriod;
    private FiscalPeriodRef closedPeriod;

    @BeforeEach
    void setUp() {
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
        when(valuationBatchPersistencePort.save(any(ValuationBatch.class))).thenAnswer(invocation -> {
            ValuationBatch batch = invocation.getArgument(0);
            if (batch.getId() == null) {
                batch.setId(77L);
            }
            return batch;
        });
        when(journalPostingPort.createDraftEntry(any(JournalEntryCommand.class)))
                .thenReturn(new JournalPostingResult(900L, "SLIP-900", "DRAFT"));

        // when
        ValuationBatch result = closingService.runValuationBatch(
                1L,
                ValuationBatch.ValuationType.FX_RATE,
                "ADMIN");

        // then
        assertThat(result.getStatus()).isEqualTo(ValuationBatch.ValuationBatchStatus.COMPLETED);
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
        when(provisionBatchPersistencePort.save(any(ProvisionBatch.class))).thenAnswer(invocation -> {
            ProvisionBatch batch = invocation.getArgument(0);
            if (batch.getId() == null) {
                batch.setId(88L);
            }
            return batch;
        });
        when(journalPostingPort.createDraftEntry(any(JournalEntryCommand.class)))
                .thenReturn(new JournalPostingResult(901L, "SLIP-901", "DRAFT"));

        // when
        ProvisionBatch result = closingService.runProvisionBatch(
                1L,
                ProvisionBatch.ProvisionType.BAD_DEBT,
                "ADMIN");

        // then
        assertThat(result.getStatus()).isEqualTo(ProvisionBatch.ProvisionBatchStatus.COMPLETED);
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

        verifyNoInteractions(valuationBatchPersistencePort, journalPostingPort);
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
}
