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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    @ParameterizedTest
    @CsvSource({
            "4808.5714, 0.00, 4808.57, DEBIT, 550100",
            "100.0050, 100.00, 0.01, DEBIT, 550100",
            "99.9949, 100.00, 0.01, DEBIT, 129100"
    })
    void quantizesAdditionalAndReversalPostingsToJournalPrecision(
            String target, String existing, String amount, String side, String firstAccount) {
        LocalDate date = LocalDate.of(2026, 5, 31);
        when(eclAllowanceResultPort.loadSummaries(date)).thenReturn(List.of(summary(
                date, "RUN-A", "KRW", "12000", "129100", "550100", "480100", target)));
        when(allowanceBalanceLookupPort.findCreditEndingBalance("129100", "KRW", date))
                .thenReturn(new BigDecimal(existing));
        when(closingJournalEntryPort.createDraftAdjustment(any()))
                .thenReturn(new ClosingJournalEntryResult(901L, "ECL-TEST"));

        service.processEclProvision(date, 690L);

        var captor = ArgumentCaptor.forClass(ClosingJournalEntryCommand.class);
        verify(closingJournalEntryPort).createDraftAdjustment(captor.capture());
        var lines = captor.getValue().lines();
        assertLine(lines.get(0), ClosingJournalSide.valueOf(side), firstAccount, amount);
        assertThat(lines.get(0).amount().scale()).isEqualTo(2);
        assertThat(lines.get(1).amount()).isEqualTo(lines.get(0).amount());
        assertThat(lines.get(1).side()).isNotEqualTo(lines.get(0).side());
    }

    @Test
    void roundsAfterSummingExposuresAndSubtractingExistingAllowanceOnlyOnce() {
        LocalDate date = LocalDate.of(2026, 5, 31);
        when(eclAllowanceResultPort.loadSummaries(date)).thenReturn(List.of(
                summary(date, "RUN-A", "KRW", "12000", "129100", "550100", "480100", "50.004"),
                summary(date, "RUN-A", "KRW", "12100", "129100", "550100", "480100", "50.004")));
        when(allowanceBalanceLookupPort.findCreditEndingBalance("129100", "KRW", date))
                .thenReturn(new BigDecimal("100.00"));
        when(closingJournalEntryPort.createDraftAdjustment(any()))
                .thenReturn(new ClosingJournalEntryResult(901L, "ECL-TEST"));

        service.processEclProvision(date, 690L);

        var captor = ArgumentCaptor.forClass(ClosingJournalEntryCommand.class);
        verify(closingJournalEntryPort).createDraftAdjustment(captor.capture());
        assertLine(captor.getValue().lines().get(0), ClosingJournalSide.DEBIT, "550100", "0.01");
    }

    @ParameterizedTest
    @CsvSource({"100.0049", "99.9951", "99.9950"})
    void subCentDifferenceDoesNotCreateZeroAmountJournal(String target) {
        LocalDate date = LocalDate.of(2026, 5, 31);
        when(eclAllowanceResultPort.loadSummaries(date)).thenReturn(List.of(summary(
                date, "RUN-A", "KRW", "12000", "129100", "550100", "480100", target)));
        when(allowanceBalanceLookupPort.findCreditEndingBalance("129100", "KRW", date))
                .thenReturn(new BigDecimal("100.00"));

        service.processEclProvision(date, 690L);

        verifyNoInteractions(closingJournalEntryPort);
    }

    @Test
    void invalidGlPrecisionFailsBeforeCreatingAnAdjustment() {
        LocalDate date = LocalDate.of(2026, 5, 31);
        when(eclAllowanceResultPort.loadSummaries(date)).thenReturn(List.of(summary(
                date, "RUN-A", "KRW", "12000", "129100", "550100", "480100", "200.00")));
        when(allowanceBalanceLookupPort.findCreditEndingBalance("129100", "KRW", date))
                .thenReturn(new BigDecimal("100.001"));

        assertThatThrownBy(() -> service.processEclProvision(date, 690L))
                .isInstanceOf(ArithmeticException.class);

        verifyNoInteractions(closingJournalEntryPort);
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
