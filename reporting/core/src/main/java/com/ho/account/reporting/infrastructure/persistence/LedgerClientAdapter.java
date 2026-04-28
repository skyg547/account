package com.ho.account.reporting.infrastructure.persistence;

import com.ho.account.reporting.application.port.out.LoadLedgerPort;
import com.ho.account.reporting.application.port.out.LoadReportHistoryPort;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.ReportLine;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adapter that provides ledger balances and historical reporting snapshots.
 */
@Component
public class LedgerClientAdapter implements LoadLedgerPort, LoadReportHistoryPort {

    @Override
    public Map<String, BigDecimal> getAccountBalances(LocalDateTime baseDate) {
        Map<String, BigDecimal> mockBalances = new HashMap<>();
        mockBalances.put("101", new BigDecimal("500000000"));
        mockBalances.put("102", new BigDecimal("350000000"));
        return mockBalances;
    }

    @Override
    public Optional<FinancialStatement> findFinalizedStatement(
            FinancialStatement.StatementType type,
            LocalDateTime date) {
        FinancialStatement pastStatement = new FinancialStatement("PAST-001", type, date);
        pastStatement.addLine(new ReportLine(
                "ASSET_CASH",
                "Cash and cash equivalents",
                new BigDecimal("700000000"),
                BigDecimal.ZERO,
                "3",
                1));
        pastStatement.finalizeStatement();
        return Optional.of(pastStatement);
    }
}
