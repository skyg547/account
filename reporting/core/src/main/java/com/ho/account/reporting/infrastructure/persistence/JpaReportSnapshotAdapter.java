package com.ho.account.reporting.infrastructure.persistence;

import com.ho.account.reporting.application.port.out.LoadReportHistoryPort;
import com.ho.account.reporting.application.port.out.StoreReportSnapshotPort;
import com.ho.account.reporting.domain.model.FinancialStatement;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "account.reporting.persistence", name = "mode", havingValue = "jpa", matchIfMissing = true)
public class JpaReportSnapshotAdapter implements LoadReportHistoryPort, StoreReportSnapshotPort {

    private final ReportSnapshotHeaderJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<FinancialStatement> findFinalizedStatement(
            FinancialStatement.StatementType type,
            LocalDateTime date) {
        return repository.findByStatementTypeAndBaseDateAndStatus(
                        type.name(),
                        date,
                        FinancialStatement.StatementStatus.FINAL.name())
                .map(ReportSnapshotHeaderJpaEntity::toDomain);
    }

    @Override
    @Transactional
    public void saveFinalized(FinancialStatement statement) {
        if (statement == null || statement.getStatus() != FinancialStatement.StatementStatus.FINAL) {
            return;
        }

        repository.deleteByStatementTypeAndBaseDateAndStatus(
                statement.getType().name(),
                statement.getBaseDate(),
                FinancialStatement.StatementStatus.FINAL.name());
        repository.flush();
        repository.save(ReportSnapshotHeaderJpaEntity.from(statement));
    }
}
