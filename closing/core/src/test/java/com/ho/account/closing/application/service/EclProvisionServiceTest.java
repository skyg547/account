package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.out.AllowanceBalanceLookupPort;
import com.ho.account.closing.application.port.out.ClosingJournalEntryCommand;
import com.ho.account.closing.application.port.out.ClosingJournalEntryPort;
import com.ho.account.closing.application.port.out.ClosingJournalEntryResult;
import com.ho.account.closing.application.port.out.ClosingJournalLineCommand;
import com.ho.account.closing.application.port.out.ClosingJournalSide;
import com.ho.account.closing.application.port.out.EclAllowanceResultPort;
import com.ho.account.closing.domain.EclAllowanceSummary;
import com.ho.account.closing.domain.ProvisionBatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EclProvisionServiceTest {

    @Mock
    private AllowanceBalanceLookupPort allowanceBalanceLookupPort;
    @Mock
    private ClosingJournalEntryPort closingJournalEntryPort;
    @Mock
    private EclAllowanceResultPort eclAllowanceResultPort;

    private ClosingAccountingProperties accountingProperties;
    private EclProvisionService service;

    @BeforeEach
    void setUp() {
        accountingProperties = new ClosingAccountingProperties();
        accountingProperties.setProvisionRules(Map.of(
                ProvisionBatch.ProvisionType.ECL,
                rule("550100", "129100")));
        service = new EclProvisionService(
                allowanceBalanceLookupPort,
                closingJournalEntryPort,
                accountingProperties,
                eclAllowanceResultPort);
    }

    @Test
    void processEclProvisionUsesFinalizedAllowanceSummaryInsteadOfFixedRate() {
        accountingProperties.setAutoPostAdjustments(true);
        LocalDate closingDate = LocalDate.of(2026, 5, 31);
        EclAllowanceSummary summary = summary(
                closingDate,
                "RUN-202605",
                "USD",
                "12000",
                "129100",
                null,
                "480100",
                "1000.00");
        when(eclAllowanceResultPort.loadSummaries(closingDate)).thenReturn(List.of(summary));
        when(allowanceBalanceLookupPort.findCreditEndingBalance("129100", "USD", closingDate))
                .thenReturn(new BigDecimal("800.00"));
        when(closingJournalEntryPort.createDraftAdjustment(any(ClosingJournalEntryCommand.class)))
                .thenReturn(new ClosingJournalEntryResult(901L, "ECL2026053144ABC"));

        service.processEclProvision(closingDate, 44L);

        ArgumentCaptor<ClosingJournalEntryCommand> captor = ArgumentCaptor.forClass(ClosingJournalEntryCommand.class);
        verify(closingJournalEntryPort).createDraftAdjustment(captor.capture());
        ClosingJournalEntryCommand command = captor.getValue();
        assertThat(command.currencyCode()).isEqualTo("USD");
        assertThat(command.slipDate()).isEqualTo(closingDate);
        assertThat(command.accountingDate()).isEqualTo(closingDate);
        assertThat(command.lineageSourceId()).isEqualTo("44|129100|USD");
        assertThat(command.slipNo()).startsWith("ECL20260531").hasSize(20);
        assertThat(command.lines()).hasSize(2);
        assertLine(command.lines().get(0), ClosingJournalSide.DEBIT, "550100", "200.00");
        assertLine(command.lines().get(1), ClosingJournalSide.CREDIT, "129100", "200.00");
        verify(closingJournalEntryPort).approveAndPost(901L, "SYSTEM");
    }

    @Test
    void processEclProvisionUsesSummaryReversalIncomeAccountForAllowanceRelease() {
        LocalDate closingDate = LocalDate.of(2026, 5, 31);
        EclAllowanceSummary summary = summary(
                closingDate,
                "RUN-202605",
                "KRW",
                "12000",
                "129100",
                "550100",
                "480100",
                "800.00");
        when(eclAllowanceResultPort.loadSummaries(closingDate)).thenReturn(List.of(summary));
        when(allowanceBalanceLookupPort.findCreditEndingBalance("129100", "KRW", closingDate))
                .thenReturn(new BigDecimal("1000.00"));
        when(closingJournalEntryPort.createDraftAdjustment(any(ClosingJournalEntryCommand.class)))
                .thenReturn(new ClosingJournalEntryResult(902L, "ECL2026053145DEF"));

        service.processEclProvision(closingDate, 45L);

        ArgumentCaptor<ClosingJournalEntryCommand> captor = ArgumentCaptor.forClass(ClosingJournalEntryCommand.class);
        verify(closingJournalEntryPort).createDraftAdjustment(captor.capture());
        ClosingJournalEntryCommand command = captor.getValue();
        assertThat(command.lines()).hasSize(2);
        assertLine(command.lines().get(0), ClosingJournalSide.DEBIT, "129100", "200.00");
        assertLine(command.lines().get(1), ClosingJournalSide.CREDIT, "480100", "200.00");
    }

    @Test
    void processEclProvisionFailsWhenNoAllowanceSummaryExists() {
        LocalDate closingDate = LocalDate.of(2026, 5, 31);
        when(eclAllowanceResultPort.loadSummaries(closingDate)).thenReturn(List.of());

        assertThatThrownBy(() -> service.processEclProvision(closingDate, 44L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("zero-portfolio completion marker");

        verifyNoInteractions(allowanceBalanceLookupPort, closingJournalEntryPort);
    }

    @Test
    void processEclProvisionRejectsNonPositiveBatchIdBeforeReadingInput() {
        assertThatThrownBy(() -> service.processEclProvision(LocalDate.of(2026, 5, 31), 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provisionBatchId");

        verifyNoInteractions(eclAllowanceResultPort, allowanceBalanceLookupPort, closingJournalEntryPort);
    }

    @Test
    void processEclProvisionAggregatesTargetsBeforeSubtractingExistingAllowance() {
        LocalDate closingDate = LocalDate.of(2026, 5, 31);
        EclAllowanceSummary first = summary(
                closingDate, "RUN-A", "USD", "12000", "129100", "550100", "480100", "600.00");
        EclAllowanceSummary second = summary(
                closingDate, "RUN-A", "USD", "12100", "129100", "550100", "480100", "400.00");
        when(eclAllowanceResultPort.loadSummaries(closingDate)).thenReturn(List.of(first, second));
        when(allowanceBalanceLookupPort.findCreditEndingBalance("129100", "USD", closingDate))
                .thenReturn(new BigDecimal("800.00"));
        when(closingJournalEntryPort.createDraftAdjustment(any(ClosingJournalEntryCommand.class)))
                .thenReturn(new ClosingJournalEntryResult(903L, "ECL20260531ABCDEF123"));

        service.processEclProvision(closingDate, 46L);

        ArgumentCaptor<ClosingJournalEntryCommand> captor =
                ArgumentCaptor.forClass(ClosingJournalEntryCommand.class);
        verify(closingJournalEntryPort).createDraftAdjustment(captor.capture());
        assertLine(captor.getValue().lines().get(0), ClosingJournalSide.DEBIT, "550100", "200.00");
        assertLine(captor.getValue().lines().get(1), ClosingJournalSide.CREDIT, "129100", "200.00");
        verify(allowanceBalanceLookupPort)
                .findCreditEndingBalance("129100", "USD", closingDate);
    }

    @Test
    void processEclProvisionRejectsMismatchedSummaryDate() {
        LocalDate closingDate = LocalDate.of(2026, 5, 31);
        EclAllowanceSummary summary = summary(
                closingDate.minusDays(1),
                "RUN-A",
                "USD",
                "12000",
                "129100",
                "550100",
                "480100",
                "600.00");
        when(eclAllowanceResultPort.loadSummaries(closingDate)).thenReturn(List.of(summary));

        assertThatThrownBy(() -> service.processEclProvision(closingDate, 46L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not match closingDate");
    }

    private static EclAllowanceSummary summary(LocalDate baseDate,
                                               String runId,
                                               String currencyCode,
                                               String exposureAccountCode,
                                               String allowanceAccountCode,
                                               String expenseAccountCode,
                                               String reversalIncomeAccountCode,
                                               String targetAllowanceAmount) {
        return new EclAllowanceSummary(
                baseDate,
                runId,
                "IFRS9-2026.05",
                "MODERN",
                currencyCode,
                exposureAccountCode,
                allowanceAccountCode,
                expenseAccountCode,
                reversalIncomeAccountCode,
                new BigDecimal(targetAllowanceAmount),
                new BigDecimal("100000.00"),
                new BigDecimal("300.00"),
                new BigDecimal("400.00"),
                new BigDecimal("300.00"));
    }

    private static ClosingAccountingProperties.AutomatedJournalRule rule(
            String debitAccountCode,
            String creditAccountCode) {
        ClosingAccountingProperties.AutomatedJournalRule rule =
                new ClosingAccountingProperties.AutomatedJournalRule();
        rule.setDebitAccountCode(debitAccountCode);
        rule.setCreditAccountCode(creditAccountCode);
        rule.setAmount(BigDecimal.ONE);
        return rule;
    }

    private static void assertLine(ClosingJournalLineCommand line, ClosingJournalSide side, String accountCode, String amount) {
        assertThat(line.side()).isEqualTo(side);
        assertThat(line.accountCode()).isEqualTo(accountCode);
        assertThat(line.amount()).isEqualByComparingTo(amount);
        assertThat(line.baseAmount()).isEqualByComparingTo(amount);
    }
}
