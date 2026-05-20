package com.ho.account.reporting.infrastructure.persistence;

import com.ho.account.contracts.ledger.LedgerBalanceSummary;
import com.ho.account.contracts.ledger.LedgerQueryPort;
import com.ho.account.reporting.application.port.out.LoadLedgerPort;
import com.ho.account.reporting.application.port.out.LoadReportHistoryPort;
import com.ho.account.reporting.domain.model.FinancialStatement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Adapter that provides ledger balances and historical reporting snapshots.
 */
@Component
public class LedgerClientAdapter implements LoadLedgerPort, LoadReportHistoryPort {

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

    @Override
    public Optional<FinancialStatement> findFinalizedStatement(
            FinancialStatement.StatementType type,
            LocalDateTime date) {
        // @todo [검수-헥사고날/업무프로세스] 전기 비교와 제출본 재현을 위해 FINAL 스냅샷 저장소 포트를 실제 JPA/외부 저장소
        //       어댑터로 연결해야 한다. 현재 Optional.empty()는 비교 재무제표와 감사 증빙 요건을 충족하지 못한다.
        return Optional.empty();
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
