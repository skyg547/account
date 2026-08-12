package com.ho.account.common.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ho.account.contracts.ledger.LedgerBalanceSummary;
import com.ho.account.journalledger.application.service.ledger.LedgerService;
import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import com.ho.account.journalledger.domain.ledger.domain.SlBalance;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MonolithLedgerQueryAdapterTest {

    @Mock
    private LedgerService ledgerService;

    @Test
    void getGlBalanceSummariesMapsGlBalances() {
        LocalDate startDate = LocalDate.of(2026, 5, 1);
        LocalDate endDate = LocalDate.of(2026, 5, 31);
        MonolithLedgerQueryAdapter adapter = new MonolithLedgerQueryAdapter(ledgerService);
        GlBalance balance = glBalance("101000", "KRW", "1000.00", "250.00", "750.00");

        when(ledgerService.getGlBalances(startDate, endDate, "101000", "KRW"))
                .thenReturn(List.of(balance));

        List<LedgerBalanceSummary> summaries = adapter.getGlBalanceSummaries(
                startDate,
                endDate,
                "101000",
                "KRW");

        assertThat(summaries).hasSize(1);
        LedgerBalanceSummary summary = summaries.get(0);
        assertThat(summary.getAccountCode()).isEqualTo("101000");
        assertThat(summary.getCurrencyCode()).isEqualTo("KRW");
        assertThat(summary.getDebitAmount()).isEqualByComparingTo("1000.00");
        assertThat(summary.getCreditAmount()).isEqualByComparingTo("250.00");
        assertThat(summary.getEndingBalance()).isEqualByComparingTo("750.00");
    }

    @Test
    void getSlBalanceSummariesMapsPartnerAndDepartmentDimensions() {
        LocalDate startDate = LocalDate.of(2026, 5, 1);
        LocalDate endDate = LocalDate.of(2026, 5, 31);
        MonolithLedgerQueryAdapter adapter = new MonolithLedgerQueryAdapter(ledgerService);
        SlBalance balance = slBalance("121000", "BP-001", "D-001", "KRW", "500.00", "100.00", "400.00");

        when(ledgerService.getSlBalances(startDate, endDate, "121000", "BP-001", "D-001", "KRW"))
                .thenReturn(List.of(balance));

        List<LedgerBalanceSummary> summaries = adapter.getSlBalanceSummaries(
                startDate,
                endDate,
                "121000",
                "BP-001",
                "D-001",
                "KRW");

        assertThat(summaries).hasSize(1);
        LedgerBalanceSummary summary = summaries.get(0);
        assertThat(summary.getAccountCode()).isEqualTo("121000");
        assertThat(summary.getBusinessPartnerCode()).isEqualTo("BP-001");
        assertThat(summary.getDepartmentCode()).isEqualTo("D-001");
        assertThat(summary.getCurrencyCode()).isEqualTo("KRW");
        assertThat(summary.getDebitAmount()).isEqualByComparingTo("500.00");
        assertThat(summary.getCreditAmount()).isEqualByComparingTo("100.00");
        assertThat(summary.getEndingBalance()).isEqualByComparingTo("400.00");
    }

    @Test
    void calculateLedgerSummaryDelegatesToLedgerService() {
        LocalDate date = LocalDate.of(2026, 5, 11);
        MonolithLedgerQueryAdapter adapter = new MonolithLedgerQueryAdapter(ledgerService);
        com.ho.account.contracts.ledger.LedgerAggregateSummary expectedSummary =
                new com.ho.account.contracts.ledger.LedgerAggregateSummary(5L, new BigDecimal("1500.00"));

        when(ledgerService.calculateGlBalanceAggregate(date, date, "101000", "KRW", "DEBIT"))
                .thenReturn(expectedSummary);

        com.ho.account.contracts.ledger.LedgerAggregateSummary summary =
                adapter.calculateLedgerSummary(date, date, "101000", "KRW", "DEBIT");

        assertThat(summary.getCount()).isEqualTo(5L);
        assertThat(summary.getTotalAmount()).isEqualByComparingTo("1500.00");
    }

    private GlBalance glBalance(
            String accountCode,
            String currencyCode,
            String debitAmount,
            String creditAmount,
            String endingBalance) {
        GlBalance balance = new GlBalance();
        balance.setAccountCode(accountCode);
        balance.setCurrencyCode(currencyCode);
        balance.setDebitAmount(new BigDecimal(debitAmount));
        balance.setCreditAmount(new BigDecimal(creditAmount));
        balance.setEndingBalance(new BigDecimal(endingBalance));
        return balance;
    }

    private SlBalance slBalance(
            String accountCode,
            String businessPartnerCode,
            String departmentCode,
            String currencyCode,
            String debitAmount,
            String creditAmount,
            String endingBalance) {
        SlBalance balance = new SlBalance();
        balance.setAccountCode(accountCode);
        balance.setBusinessPartnerCode(businessPartnerCode);
        balance.setDepartmentCode(departmentCode);
        balance.setCurrencyCode(currencyCode);
        balance.setDebitAmount(new BigDecimal(debitAmount));
        balance.setCreditAmount(new BigDecimal(creditAmount));
        balance.setEndingBalance(new BigDecimal(endingBalance));
        return balance;
    }
}
