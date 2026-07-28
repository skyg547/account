package com.ho.account.reporting.infrastructure.persistence;

import com.ho.account.reporting.domain.model.FinancialStatement;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rpt_snapshot_header")
class ReportSnapshotHeaderJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "statement_id", nullable = false, length = 80)
    private String statementId;

    @Column(name = "statement_type", nullable = false, length = 40)
    private String statementType;

    @Column(name = "base_date", nullable = false)
    private LocalDateTime baseDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "header", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineOrder ASC")
    private List<ReportSnapshotDetailJpaEntity> details = new ArrayList<>();

    protected ReportSnapshotHeaderJpaEntity() {
    }

    static ReportSnapshotHeaderJpaEntity from(FinancialStatement statement) {
        ReportSnapshotHeaderJpaEntity header = new ReportSnapshotHeaderJpaEntity();
        header.statementId = statement.getStatementId();
        header.statementType = statement.getType().name();
        header.baseDate = statement.getBaseDate();
        header.status = statement.getStatus().name();
        header.createdAt = LocalDateTime.now();

        for (int i = 0; i < statement.getLines().size(); i++) {
            header.addDetail(ReportSnapshotDetailJpaEntity.from(statement.getLines().get(i), i + 1));
        }
        return header;
    }

    FinancialStatement toDomain() {
        FinancialStatement statement = new FinancialStatement(
                statementId,
                FinancialStatement.StatementType.valueOf(statementType),
                baseDate);
        details.stream()
                .map(ReportSnapshotDetailJpaEntity::toDomain)
                .forEach(statement::addLine);

        if (FinancialStatement.StatementStatus.FINAL.name().equals(status)) {
            statement.finalizeStatement();
        }
        return statement;
    }

    private void addDetail(ReportSnapshotDetailJpaEntity detail) {
        detail.assignHeader(this);
        this.details.add(detail);
    }
}
