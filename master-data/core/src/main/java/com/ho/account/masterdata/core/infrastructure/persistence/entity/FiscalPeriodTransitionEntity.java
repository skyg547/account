package com.ho.account.masterdata.core.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AccessLevel;
import org.springframework.data.domain.Persistable;

/** Durable identity and exact payload for a Closing fiscal transition. */
@Entity
@Table(name = "fiscal_period_transitions")
@Getter
@Setter
@NoArgsConstructor
public class FiscalPeriodTransitionEntity implements Persistable<String> {
    public enum Phase { PREPARED, APPLIED, CANCELLED, CONFIRMED }

    @Id
    @Column(name = "operation_id", nullable = false, length = 128)
    private String operationId;

    @Column(name = "fiscal_period_id", nullable = false, updatable = false)
    private Long fiscalPeriodId;

    @Column(name = "expected_status", nullable = false, length = 20, updatable = false)
    private String expectedStatus;

    @Column(name = "target_status", nullable = false, length = 20, updatable = false)
    private String targetStatus;

    @Column(name = "audit_user", nullable = false, length = 42, updatable = false)
    private String auditUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false, length = 12)
    private Phase phase;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    // Assigned IDs must still INSERT. merge() could overwrite another period's committed receipt
    // after our missing-ID read; the primary key must arbitrate that race instead.
    @Transient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean newEntity = true;

    @Override
    public String getId() {
        return operationId;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    private void markPersisted() {
        newEntity = false;
    }
}
