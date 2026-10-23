package com.ho.account.reporting.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.contracts.ledger.LedgerBalanceSummary;
import com.ho.account.contracts.ledger.LedgerQueryPort;
import com.ho.account.reporting.application.port.in.GenerateStatementUseCase.GenerateCommand;
import com.ho.account.reporting.application.service.ReportingService;
import com.ho.account.reporting.domain.model.FinancialStatement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class LedgerClientAdapterTest {

    @Test
    void getAccountBalances_loadsGlBalancesFromLedgerQueryPort() {
        RecordingLedgerQueryPort ledgerQueryPort = new RecordingLedgerQueryPort(List.of(
                summary("101", "KRW", null, null, "500000000"),
                summary("102", "KRW", null, null, "350000000"),
                summary("101", "KRW", null, null, "25000000"),
                summary(null, "KRW", null, null, "999")));
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
                summary("401", "KRW", "100000", "25000", null)));
        LedgerClientAdapter adapter = new LedgerClientAdapter(ledgerQueryPort);

        Map<String, BigDecimal> balances = adapter.getAccountBalances(
                LocalDateTime.of(2026, 3, 31, 0, 0));

        assertThat(balances.get("401")).isEqualByComparingTo("75000");
    }

    @Test
    void getAccountBalances_rejectsMixedKrwAndUsdForSameAccount() {
        LedgerClientAdapter adapter = new LedgerClientAdapter(new RecordingLedgerQueryPort(List.of(
                summary("101", "KRW", null, null, "100"),
                summary("101", "USD", null, null, "20"))));

        assertThatThrownBy(() -> adapter.getAccountBalances(LocalDateTime.of(2026, 3, 31, 0, 0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("USD");
    }

    @Test
    void getAccountBalances_rejectsUsdOnlyAccount() {
        LedgerClientAdapter adapter = new LedgerClientAdapter(new RecordingLedgerQueryPort(List.of(
                summary("101", "USD", null, null, "20"))));

        assertThatThrownBy(() -> adapter.getAccountBalances(LocalDateTime.of(2026, 3, 31, 0, 0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("USD");
    }

    @Test
    void getAccountBalances_rejectsMissingCurrency() {
        LedgerClientAdapter adapter = new LedgerClientAdapter(new RecordingLedgerQueryPort(List.of(
                summary("101", null, null, null, "100"))));

        assertThatThrownBy(() -> adapter.getAccountBalances(LocalDateTime.of(2026, 3, 31, 0, 0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("currency");
    }

    @Test
    void generate_doesNotSaveFinalSnapshotWhenLedgerCurrencyIsRejected() {
        LedgerClientAdapter adapter = new LedgerClientAdapter(new RecordingLedgerQueryPort(List.of(
                summary("101", "USD", null, null, "20"))));
        AtomicBoolean snapshotSaved = new AtomicBoolean();
        ReportingService service = new ReportingService(
                adapter,
                (type, date) -> Optional.empty(),
                (type, date) -> List.of(),
                statement -> snapshotSaved.set(true));

        assertThatThrownBy(() -> service.generate(new GenerateCommand(
                FinancialStatement.StatementType.BALANCE_SHEET,
                LocalDateTime.of(2026, 3, 31, 0, 0),
                "tester")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("USD");
        assertThat(snapshotSaved).isFalse();
    }

    private static LedgerBalanceSummary summary(
            String accountCode,
            String currencyCode,
            String debitAmount,
            String creditAmount,
            String endingBalance) {
        LedgerBalanceSummary summary = new LedgerBalanceSummary();
        summary.setAccountCode(accountCode);
        summary.setCurrencyCode(currencyCode);
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
