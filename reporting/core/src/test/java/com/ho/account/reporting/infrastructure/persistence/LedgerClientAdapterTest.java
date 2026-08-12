package com.ho.account.reporting.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.contracts.ledger.LedgerBalanceSummary;
import com.ho.account.contracts.ledger.LedgerQueryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LedgerClientAdapterTest {

    @Test
    void getAccountBalances_loadsGlBalancesFromLedgerQueryPort() {
        RecordingLedgerQueryPort ledgerQueryPort = new RecordingLedgerQueryPort(List.of(
                summary("101", null, null, "500000000"),
                summary("102", null, null, "350000000"),
                summary("101", null, null, "25000000"),
                summary(null, null, null, "999")));
        LedgerClientAdapter adapter = new LedgerClientAdapter(ledgerQueryPort);

        Map<String, BigDecimal> balances = adapter.getAccountBalances(
                LocalDateTime.of(2026, 3, 31, 23, 59));

        assertThat(ledgerQueryPort.startDate).isEqualTo(LocalDate.of(2026, 3, 31));
        assertThat(ledgerQueryPort.endDate).isEqualTo(LocalDate.of(2026, 3, 31));
        assertThat(ledgerQueryPort.accountCode).isNull();
        assertThat(ledgerQueryPort.currencyCode).isNull();
        assertThat(balances).containsOnlyKeys("101", "102");
        assertThat(balances.get("101")).isEqualByComparingTo("525000000");
        assertThat(balances.get("102")).isEqualByComparingTo("350000000");
    }

    @Test
    void getAccountBalances_fallsBackToDebitMinusCreditWhenEndingBalanceIsMissing() {
        RecordingLedgerQueryPort ledgerQueryPort = new RecordingLedgerQueryPort(List.of(
                summary("401", "100000", "25000", null)));
        LedgerClientAdapter adapter = new LedgerClientAdapter(ledgerQueryPort);

        Map<String, BigDecimal> balances = adapter.getAccountBalances(
                LocalDateTime.of(2026, 3, 31, 0, 0));

        assertThat(balances.get("401")).isEqualByComparingTo("75000");
    }

    private static LedgerBalanceSummary summary(
            String accountCode,
            String debitAmount,
            String creditAmount,
            String endingBalance) {
        LedgerBalanceSummary summary = new LedgerBalanceSummary();
        summary.setAccountCode(accountCode);
        summary.setDebitAmount(decimal(debitAmount));
        summary.setCreditAmount(decimal(creditAmount));
        summary.setEndingBalance(decimal(endingBalance));
        return summary;
    }

    private static BigDecimal decimal(String amount) {
        return amount != null ? new BigDecimal(amount) : null;
    }

    private static class RecordingLedgerQueryPort implements LedgerQueryPort {

        private final List<LedgerBalanceSummary> response;
        private LocalDate startDate;
        private LocalDate endDate;
        private String accountCode;
        private String currencyCode;

        private RecordingLedgerQueryPort(List<LedgerBalanceSummary> response) {
            this.response = response;
        }

        @Override
        public List<LedgerBalanceSummary> getGlBalanceSummaries(
                LocalDate startDate,
                LocalDate endDate,
                String accountCode,
                String currencyCode) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.accountCode = accountCode;
            this.currencyCode = currencyCode;
            return response;
        }

        @Override
        public List<LedgerBalanceSummary> getSlBalanceSummaries(
                LocalDate startDate,
                LocalDate endDate,
                String accountCode,
                String businessPartnerCode,
                String departmentCode,
                String currencyCode) {
            return List.of();
        }

        @Override
        public com.ho.account.contracts.ledger.LedgerAggregateSummary calculateLedgerSummary(
                LocalDate startDate,
                LocalDate endDate,
                String accountCode,
                String currencyCode,
                String amountBasis) {
            return new com.ho.account.contracts.ledger.LedgerAggregateSummary(0L, java.math.BigDecimal.ZERO);
        }
    }
}
