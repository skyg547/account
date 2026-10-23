package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.domain.FinalCloseEvidenceControl;
import com.ho.account.closing.domain.FinalCloseEvidenceTotal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "final_close_evidence_controls")
class FinalCloseEvidenceControlEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evidence_set_db_id", nullable = false, updatable = false)
    private Long evidenceSetDbId;

    @Enumerated(EnumType.STRING)
    @Column(name = "control_type", nullable = false, length = 40, updatable = false)
    private FinalCloseEvidenceControl.Type type;

    @Column(name = "source_system", nullable = false, length = 100, updatable = false)
    private String sourceSystem;

    @Column(name = "source_run_id", nullable = false, length = 100, updatable = false)
    private String sourceRunId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 10, updatable = false)
    private FinalCloseEvidenceControl.Outcome outcome;

    @Column(name = "blocking_item_count", nullable = false, updatable = false)
    private int blockingItemCount;

    protected FinalCloseEvidenceControlEntity() {
    }

    FinalCloseEvidenceControlEntity(Long evidenceSetDbId, FinalCloseEvidenceControl control) {
        this.evidenceSetDbId = evidenceSetDbId;
        this.type = control.type();
        this.sourceSystem = control.sourceSystem();
        this.sourceRunId = control.sourceRunId();
        this.outcome = control.outcome();
        this.blockingItemCount = control.blockingItemCount();
    }

    Long getId() {
        return id;
    }

    FinalCloseEvidenceControl toDomain(List<FinalCloseEvidenceTotal> totals) {
        return new FinalCloseEvidenceControl(
                type,
                sourceSystem,
                sourceRunId,
                outcome,
                blockingItemCount,
                totals);
    }
}
