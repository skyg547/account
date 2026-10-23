package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.out.AllowanceBalanceLookupPort;
import com.ho.account.closing.application.port.out.AllowanceBalance;
import com.ho.account.closing.application.port.out.FxExchangeRateLookupPort;
import com.ho.account.closing.application.port.out.ClosingJournalEntryCommand;
import com.ho.account.closing.application.port.out.ClosingJournalEntryPort;
import com.ho.account.closing.application.port.out.ClosingJournalEntryResult;
import com.ho.account.closing.application.port.out.ClosingJournalLineCommand;
import com.ho.account.closing.application.port.out.ClosingJournalLineage;
import com.ho.account.closing.application.port.out.ClosingJournalSide;
import com.ho.account.closing.application.port.out.EclAllowanceResultPort;
import com.ho.account.closing.application.port.out.EclProvisionSnapshotPort;
import com.ho.account.closing.domain.EclAllowanceSummary;
import com.ho.account.closing.domain.EclProvisionSnapshot;
import com.ho.account.closing.domain.ProvisionBatch;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
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

    @Mock
    private FxExchangeRateLookupPort rates;
    @Mock
    private EclProvisionSnapshotPort snapshots;
    @Captor
    private ArgumentCaptor<List<EclProvisionSnapshot>> recordedSnapshots;

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
                eclAllowanceResultPort, rates, snapshots);
        org.mockito.Mockito.lenient().when(closingJournalEntryPort.preflightEclLineage(any()))
                .thenReturn(Map.of());
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
        when(allowanceBalanceLookupPort.findCreditBalance("129100", "USD", "KRW", closingDate))
                .thenReturn(balance("USD", "800.00", "1040000.00"));
        when(rates.findRate("USD", "KRW", closingDate)).thenReturn(Optional.of(new BigDecimal("1300")));
        when(closingJournalEntryPort.createDraftAdjustment(any(ClosingJournalEntryCommand.class)))
                .thenReturn(new ClosingJournalEntryResult(901L, "ECL2026053144ABC"));

        service.processEclProvision(closingDate, 44L);

        ArgumentCaptor<ClosingJournalEntryCommand> captor = ArgumentCaptor.forClass(ClosingJournalEntryCommand.class);
        verify(closingJournalEntryPort).createDraftAdjustment(captor.capture());
        ClosingJournalEntryCommand command = captor.getValue();
        assertThat(command.currencyCode()).isEqualTo("USD");
        assertThat(command.slipDate()).isEqualTo(closingDate);
        assertThat(command.accountingDate()).isEqualTo(closingDate);
        assertThat(command.lineageSourceId()).startsWith("ECLSNAP:");
        assertThat(command.slipNo()).startsWith("ECL20260531").hasSize(20);
        assertThat(command.lines()).hasSize(2);
        assertLine(command.lines().get(0), ClosingJournalSide.DEBIT, "550100", "200.00", "260000.00");
        assertLine(command.lines().get(1), ClosingJournalSide.CREDIT, "129100", "200.00", "260000.00");
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
        when(allowanceBalanceLookupPort.findCreditBalance("129100", "KRW", "KRW", closingDate))
                .thenReturn(balance("KRW", "1000.00", "1000.00"));
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
    void nullLineagePreflightFailsBeforeSnapshotReservationOrJournalWrite() {
        LocalDate date = LocalDate.of(2026, 5, 31);
        when(eclAllowanceResultPort.loadSummaries(date)).thenReturn(List.of(summary(
                date, "RUN-A", "KRW", "12000", "129100", "550100", "480100", "100.00")));
        when(allowanceBalanceLookupPort.findCreditBalance("129100", "KRW", "KRW", date))
                .thenReturn(balance("KRW", "0.00", "0.00"));
        when(closingJournalEntryPort.preflightEclLineage(any())).thenReturn(null);

        assertThatThrownBy(() -> service.processEclProvision(date, 781L))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("preflight result must not be null");
        verifyNoInteractions(snapshots);
        verify(closingJournalEntryPort, org.mockito.Mockito.never()).createDraftAdjustment(any());
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
    void explicitZeroExposureAndZeroStagesRequireNoJournalWhenBalanceIsZero() {
        LocalDate date = LocalDate.of(2026, 5, 31);
        when(eclAllowanceResultPort.loadSummaries(date)).thenReturn(List.of(new EclAllowanceSummary(
                date, "RUN-ZERO", "MODEL-ZERO", "ENTITY", "KRW", "12000", "129100",
                "550100", "480100", BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)));
        when(allowanceBalanceLookupPort.findCreditBalance("129100", "KRW", "KRW", date))
                .thenReturn(balance("KRW", "0", "0"));

        assertThat(service.processEclProvision(date, 47L)).isEmpty();

        // Zero money effects still bind source lineage so a retry cannot replace the confirmed zero snapshot.
        var order = inOrder(closingJournalEntryPort, snapshots);
        order.verify(closingJournalEntryPort).preflightEclLineage(any());
        order.verify(snapshots).recordAll(recordedSnapshots.capture(), eq(Map.of()));
        assertThat(recordedSnapshots.getValue()).hasSize(1);
        EclProvisionSnapshot recorded = recordedSnapshots.getValue().get(0);
        assertThat(recorded.baseDate()).isEqualTo(date);
        assertThat(recorded.provisionBatchId()).isEqualTo(47L);
        assertThat(recorded.runId()).isEqualTo("RUN-ZERO");
        assertThat(recorded.modelVersion()).isEqualTo("MODEL-ZERO");
        assertThat(recorded.legalEntityCode()).isEqualTo("ENTITY");
        assertThat(recorded.currencyCode()).isEqualTo("KRW");
        assertThat(recorded.allowanceAccountCode()).isEqualTo("129100");
        assertThat(List.of(recorded.targetAllowanceAmount(), recorded.sourceExposureAmount(),
                recorded.stage1AllowanceAmount(), recorded.stage2AllowanceAmount(), recorded.stage3AllowanceAmount(),
                recorded.existingTransactionAmount(), recorded.existingBaseAmount(),
                recorded.adjustmentTransactionAmount(), recorded.adjustmentBaseAmount()))
                .allSatisfy(amount -> assertThat(amount).isZero());
        verify(closingJournalEntryPort).preflightEclLineage(List.of(new ClosingJournalLineage(
                ClosingSlipNoFactory.eclProvision(date, "ENTITY|KRW|129100", 47L), date, "KRW",
                recorded.reference(), recorded.operationKey(), true)));
        verify(closingJournalEntryPort, never()).createDraftAdjustment(any());
        verify(closingJournalEntryPort, never()).approveAndPost(any(), any());
    }

    @Test
    void oversizedSnapshotIdentityFailsBeforeAnyJournalWrite() {
        LocalDate date = LocalDate.of(2026, 5, 31);
        when(eclAllowanceResultPort.loadSummaries(date)).thenReturn(List.of(new EclAllowanceSummary(
                date, "R".repeat(180), "MODEL", "ENTITY", "KRW", "12000", "129100",
                "550100", "480100", new BigDecimal("100"), new BigDecimal("1000"),
                new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO)));
        when(allowanceBalanceLookupPort.findCreditBalance("129100", "KRW", "KRW", date))
                .thenReturn(balance("KRW", "0", "0"));

        assertThatThrownBy(() -> service.processEclProvision(date, 47L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("line description exceeds reversible limit");
        verifyNoInteractions(closingJournalEntryPort);
    }

    @Test
    void oversizedExactSourceTotalsFailBeforeAnyJournalWrite() {
        LocalDate date = LocalDate.of(2026, 5, 31);
        when(eclAllowanceResultPort.loadSummaries(date)).thenReturn(List.of(new EclAllowanceSummary(
                date, "RUN", "MODEL", "ENTITY", "KRW", "12000", "129100",
                "550100", "480100", new BigDecimal("100"), new BigDecimal("9".repeat(210)),
                new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO)));
        when(allowanceBalanceLookupPort.findCreditBalance("129100", "KRW", "KRW", date))
                .thenReturn(balance("KRW", "0", "0"));

        assertThatThrownBy(() -> service.processEclProvision(date, 47L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("line description exceeds reversible limit");
        verifyNoInteractions(closingJournalEntryPort);
    }

    @Test
    void largeValidGroupedTotalsStillAllowActualJournalReversal() {
        LocalDate date = LocalDate.of(2026, 5, 31);
        String target = "99999999999999999.9003";
        String stage = "33333333333333333.3001";
        when(eclAllowanceResultPort.loadSummaries(date)).thenReturn(List.of(new EclAllowanceSummary(
                date, "RUN", "MODEL", "ENTITY", "KRW", "12000", "129100",
                "550100", "480100", new BigDecimal(target), new BigDecimal(target),
                new BigDecimal(stage), new BigDecimal(stage), new BigDecimal(stage))));
        when(allowanceBalanceLookupPort.findCreditBalance("129100", "KRW", "KRW", date))
                .thenReturn(balance("KRW", "0", "0"));

        EclProvisionService.PreparedEclProvision prepared = service.prepareEclProvisionSnapshot(date, 47L);
        assertThat(prepared.snapshots()).hasSize(1);
        assertThat(prepared.commands()).hasSize(1);
        // Reversal compatibility is checked before preparation can reserve source evidence or write a journal.
        verifyNoInteractions(snapshots, closingJournalEntryPort);
        ClosingJournalEntryCommand command = prepared.commands().get(0);
        JournalEntry posted = new JournalEntry();
        posted.setId(741L);
        posted.setSlipNo(command.slipNo());
        posted.setSlipDate(date);
        posted.setAccountingDate(date);
        posted.setDescription(command.description());
        posted.setEntryType(command.entryType());
        posted.setCurrencyCode(command.currencyCode());
        posted.setExchangeRate(command.exchangeRate());
        posted.setCreatedBy("maker");
        posted.setLineageSourceType(command.lineageSourceType());
        posted.setLineageSourceId(command.lineageSourceId());
        for (ClosingJournalLineCommand line : command.lines()) {
            JournalDetail detail = new JournalDetail();
            detail.setSide(JournalSide.valueOf(line.side().name()));
            detail.setAccountCode(line.accountCode());
            detail.setAmount(line.amount());
            detail.setBaseAmount(line.baseAmount());
            detail.setDetailDescription(line.description());
            posted.addDetail(detail);
        }
        posted.initializeDraft();
        posted.requestApproval("maker");
        posted.approve("checker");
        posted.post("poster");

        JournalEntry reversal = posted.createReversal("maker", date, "correction");
        assertThat(reversal.getDescription()).hasSizeLessThanOrEqualTo(200);
        assertThat(reversal.getDetails()).hasSize(2).allSatisfy(detail ->
                assertThat(detail.getDetailDescription()).hasSizeLessThanOrEqualTo(200));
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
        when(allowanceBalanceLookupPort.findCreditBalance("129100", "USD", "KRW", closingDate))
                .thenReturn(balance("USD", "800.00", "1040000.00"));
        when(rates.findRate("USD", "KRW", closingDate)).thenReturn(Optional.of(new BigDecimal("1300")));
        when(closingJournalEntryPort.createDraftAdjustment(any(ClosingJournalEntryCommand.class)))
                .thenReturn(new ClosingJournalEntryResult(903L, "ECL20260531ABCDEF123"));

        service.processEclProvision(closingDate, 46L);

        ArgumentCaptor<ClosingJournalEntryCommand> captor =
                ArgumentCaptor.forClass(ClosingJournalEntryCommand.class);
        verify(closingJournalEntryPort).createDraftAdjustment(captor.capture());
        assertLine(captor.getValue().lines().get(0), ClosingJournalSide.DEBIT, "550100", "200.00", "260000.00");
        assertLine(captor.getValue().lines().get(1), ClosingJournalSide.CREDIT, "129100", "200.00", "260000.00");
        verify(allowanceBalanceLookupPort)
                .findCreditBalance("129100", "USD", "KRW", closingDate);
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
        when(allowanceBalanceLookupPort.findCreditBalance("129100", "KRW", "KRW", date))
                .thenReturn(balance("KRW", existing, existing));
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
        when(allowanceBalanceLookupPort.findCreditBalance("129100", "KRW", "KRW", date))
                .thenReturn(balance("KRW", "100.00", "100.00"));
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
        when(allowanceBalanceLookupPort.findCreditBalance("129100", "KRW", "KRW", date))
                .thenReturn(balance("KRW", "100.00", "100.00"));

        service.processEclProvision(date, 690L);

        verify(closingJournalEntryPort, org.mockito.Mockito.never()).createDraftAdjustment(any());
    }

    @Test
    void invalidGlPrecisionFailsBeforeCreatingAnAdjustment() {
        LocalDate date = LocalDate.of(2026, 5, 31);
        when(eclAllowanceResultPort.loadSummaries(date)).thenReturn(List.of(summary(
                date, "RUN-A", "KRW", "12000", "129100", "550100", "480100", "200.00")));
        when(allowanceBalanceLookupPort.findCreditBalance("129100", "KRW", "KRW", date))
                .thenAnswer(invocation -> balance("KRW", "100.001", "100.001"));

        assertThatThrownBy(() -> service.processEclProvision(date, 690L))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(closingJournalEntryPort);
    }

    @Test
    void laterGroupMissingRateFailsAllGroupPreflightBeforeAnyJournalWrite() {
        LocalDate date = LocalDate.of(2026, 5, 31);
        EclAllowanceSummary usd = summary(
                date, "RUN-A", "USD", "12000", "129100", "550100", "480100", "100.00");
        EclAllowanceSummary eur = summary(
                date, "RUN-A", "EUR", "12100", "139100", "560100", "490100", "100.00");
        when(eclAllowanceResultPort.loadSummaries(date)).thenReturn(List.of(usd, eur));
        when(allowanceBalanceLookupPort.findCreditBalance("129100", "USD", "KRW", date))
                .thenReturn(balance("USD", "80.00", "104000.00"));
        when(allowanceBalanceLookupPort.findCreditBalance("139100", "EUR", "KRW", date))
                .thenReturn(new AllowanceBalance(
                        "EUR", "KRW", new BigDecimal("80.00"), new BigDecimal("120000.00")));
        when(rates.findRate("EUR", "KRW", date)).thenReturn(Optional.of(new BigDecimal("1500")));
        when(rates.findRate("USD", "KRW", date)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.processEclProvision(date, 690L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FX rate not found");

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
                new BigDecimal(targetAllowanceAmount),
                BigDecimal.ZERO,
                BigDecimal.ZERO);
    }

    private static ClosingAccountingProperties.EclAccountMapping rule(
            String debitAccountCode,
            String creditAccountCode) {
        ClosingAccountingProperties.EclAccountMapping rule =
                new ClosingAccountingProperties.EclAccountMapping();
        rule.setDebitAccountCode(debitAccountCode);
        rule.setCreditAccountCode(creditAccountCode);
        return rule;
    }

    private static AllowanceBalance balance(String currency, String amount, String base) {
        return new AllowanceBalance(currency, "KRW", new BigDecimal(amount), new BigDecimal(base));
    }

    private static void assertLine(ClosingJournalLineCommand line, ClosingJournalSide side, String accountCode, String amount) {
        assertLine(line, side, accountCode, amount, amount);
    }

    private static void assertLine(ClosingJournalLineCommand line, ClosingJournalSide side, String accountCode,
                                   String amount, String baseAmount) {
        assertThat(line.side()).isEqualTo(side);
        assertThat(line.accountCode()).isEqualTo(accountCode);
        assertThat(line.amount()).isEqualByComparingTo(amount);
        assertThat(line.baseAmount()).isEqualByComparingTo(baseAmount);
    }
}
