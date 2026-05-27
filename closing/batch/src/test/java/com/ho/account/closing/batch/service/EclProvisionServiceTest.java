package com.ho.account.closing.batch.service;

import com.ho.account.closing.application.port.out.EclAllowanceResultPort;
import com.ho.account.closing.application.service.ClosingAccountingProperties;
import com.ho.account.closing.domain.EclAllowanceSummary;
import com.ho.account.closing.domain.ProvisionBatch;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.domain.ledger.domain.GlAccountBalance;
import com.ho.account.journalledger.domain.ledger.domain.GlBalanceType;
import com.ho.account.journalledger.domain.ledger.repository.GlAccountBalanceRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EclProvisionServiceTest {

    @Mock
    private GlAccountBalanceRepository glAccountBalanceRepository;
    @Mock
    private JournalUseCase journalUseCase;
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
                glAccountBalanceRepository,
                journalUseCase,
                accountingProperties,
                eclAllowanceResultPort);
    }

    @Test
    void processEclProvisionUsesFinalizedAllowanceSummaryInsteadOfFixedRate() {
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
        when(glAccountBalanceRepository.findByAccountCodeAndCurrencyCodeAndBalanceDateAndBalanceType(
                "129100", "USD", closingDate, GlBalanceType.CREDIT))
                .thenReturn(Optional.of(balance("800.00")));
        when(journalUseCase.createJournalEntry(any(JournalEntry.class))).thenAnswer(invocation -> {
            JournalEntry entry = invocation.getArgument(0);
            entry.setId(901L);
            return entry;
        });

        service.processEclProvision(closingDate, 44L);

        ArgumentCaptor<JournalEntry> captor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalUseCase).createJournalEntry(captor.capture());
        JournalEntry entry = captor.getValue();
        assertThat(entry.getCurrencyCode()).isEqualTo("USD");
        assertThat(entry.getLineageSourceId()).isEqualTo("44|RUN-202605");
        assertThat(entry.getSlipNo()).startsWith("ECL20260531").hasSize(20);
        assertThat(entry.getDetails()).hasSize(2);
        assertLine(entry.getDetails().get(0), JournalSide.DEBIT, "550100", "200.00");
        assertLine(entry.getDetails().get(1), JournalSide.CREDIT, "129100", "200.00");
        verify(journalUseCase).approveJournalEntry(901L, "SYSTEM");
        verify(journalUseCase).postJournalEntry(901L, "SYSTEM");
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
        when(glAccountBalanceRepository.findByAccountCodeAndCurrencyCodeAndBalanceDateAndBalanceType(
                "129100", "KRW", closingDate, GlBalanceType.CREDIT))
                .thenReturn(Optional.of(balance("1000.00")));
        when(journalUseCase.createJournalEntry(any(JournalEntry.class))).thenAnswer(invocation -> {
            JournalEntry entry = invocation.getArgument(0);
            entry.setId(902L);
            return entry;
        });

        service.processEclProvision(closingDate, 45L);

        ArgumentCaptor<JournalEntry> captor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalUseCase).createJournalEntry(captor.capture());
        JournalEntry entry = captor.getValue();
        assertThat(entry.getDetails()).hasSize(2);
        assertLine(entry.getDetails().get(0), JournalSide.DEBIT, "129100", "200.00");
        assertLine(entry.getDetails().get(1), JournalSide.CREDIT, "480100", "200.00");
    }

    @Test
    void processEclProvisionSkipsWhenNoAllowanceSummaryExists() {
        LocalDate closingDate = LocalDate.of(2026, 5, 31);
        when(eclAllowanceResultPort.loadSummaries(closingDate)).thenReturn(List.of());

        service.processEclProvision(closingDate, 44L);

        verifyNoInteractions(glAccountBalanceRepository, journalUseCase);
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
                "KBANK",
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

    private static GlAccountBalance balance(String amount) {
        GlAccountBalance balance = new GlAccountBalance();
        balance.setEndingBalance(new BigDecimal(amount));
        return balance;
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

    private static void assertLine(JournalDetail line, JournalSide side, String accountCode, String amount) {
        assertThat(line.getSide()).isEqualTo(side);
        assertThat(line.getAccountCode()).isEqualTo(accountCode);
        assertThat(line.getAmount()).isEqualByComparingTo(amount);
        assertThat(line.getBaseAmount()).isEqualByComparingTo(amount);
    }
}
