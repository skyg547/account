package com.ho.account.masterdata.core.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Durable identity and exact payload for a Closing fiscal transition. */
@Entity
@Table(name = "fiscal_period_transitions")
@Getter
@Setter
@NoArgsConstructor
public class FiscalPeriodTransitionEntity {
    public enum Phase { PREPARED, APPLIED, CANCELLED, CONFIRMED }

    @Id
    @Column(name = "operation_id", nullable = false, length = 128)
    private String operationId;

    @Column(name = "fiscal_period_id", nullable = false)
    private Long fiscalPeriodId;

    @Column(name = "expected_status", nullable = false, length = 20)
    private String expectedStatus;

    @Column(name = "target_status", nullable = false, length = 20)
    private String targetStatus;

    @Column(name = "audit_user", nullable = false, length = 42)
    private String auditUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false, length = 12)
    private Phase phase;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;
}
