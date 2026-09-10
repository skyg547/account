package com.ho.account.reporting.infrastructure.persistence;

import com.ho.account.contracts.ledger.LedgerBalanceSummary;
import com.ho.account.contracts.ledger.LedgerQueryPort;
import com.ho.account.reporting.application.port.out.LoadLedgerPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adapter that provides ledger balances and historical reporting snapshots.
 */
@Component
@ConditionalOnProperty(prefix = "account.reporting.persistence", name = "mode", havingValue = "jpa", matchIfMissing = true)
public class LedgerClientAdapter implements LoadLedgerPort {

    private final LedgerQueryPort ledgerQueryPort;

    public LedgerClientAdapter(LedgerQueryPort ledgerQueryPort) {
        this.ledgerQueryPort = ledgerQueryPort;
    }

    @Override
    public Map<String, BigDecimal> getAccountBalances(LocalDateTime baseDate) {
        Objects.requireNonNull(baseDate, "baseDate must not be null");
        LocalDate balanceDate = baseDate.toLocalDate();
        List<LedgerBalanceSummary> summaries = ledgerQueryPort.getGlBalanceSummaries(
                balanceDate,
                balanceDate,
                null,
                null);

        return summaries.stream()
                .filter(Objects::nonNull)
                .filter(summary -> summary.getAccountCode() != null)
                .collect(Collectors.groupingBy(
                        LedgerBalanceSummary::getAccountCode,
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, this::resolveEndingBalance, BigDecimal::add)));
    }

    private BigDecimal resolveEndingBalance(LedgerBalanceSummary summary) {
        if (summary.getEndingBalance() != null) {
            return summary.getEndingBalance();
        }
        return safe(summary.getDebitAmount()).subtract(safe(summary.getCreditAmount()));
    }

    private BigDecimal safe(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }
}
