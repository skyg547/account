package com.ho.account.reporting.infrastructure.persistence;

import com.ho.account.reporting.application.port.out.LoadReportHistoryPort;
import com.ho.account.reporting.application.port.out.StoreReportSnapshotPort;
import com.ho.account.reporting.domain.model.FinancialStatement;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryReportSnapshotAdapter implements LoadReportHistoryPort, StoreReportSnapshotPort {

    private final Map<SnapshotKey, FinancialStatement> snapshots = new ConcurrentHashMap<>();

    @Override
    public Optional<FinancialStatement> findFinalizedStatement(
            FinancialStatement.StatementType type,
            LocalDateTime date) {
        return Optional.ofNullable(snapshots.get(new SnapshotKey(type, date)));
    }

    @Override
    public void saveFinalized(FinancialStatement statement) {
        if (statement == null || statement.getStatus() != FinancialStatement.StatementStatus.FINAL) {
            return;
        }
        snapshots.put(new SnapshotKey(statement.getType(), statement.getBaseDate()), statement);
    }

    private record SnapshotKey(
            FinancialStatement.StatementType type,
            LocalDateTime baseDate) {
    }
}
