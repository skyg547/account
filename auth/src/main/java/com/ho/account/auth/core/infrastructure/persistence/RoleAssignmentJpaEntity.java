package com.ho.account.auth.core.infrastructure.persistence;

import com.ho.account.auth.core.domain.model.RoleAssignment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "auth_role_assignments")
class RoleAssignmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "username", nullable = false)
    private AuthUserJpaEntity user;

    @Column(name = "role_code", nullable = false, length = 80)
    private String roleCode;

    @Column(name = "data_scope", nullable = false, length = 80)
    private String dataScope;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Column(name = "approved", nullable = false)
    private boolean approved;

    @Column(name = "approved_by", length = 80)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected RoleAssignmentJpaEntity() {
    }

    RoleAssignmentJpaEntity(
            String roleCode,
            String dataScope,
            Instant validFrom,
            Instant validTo,
            boolean approved,
            String approvedBy,
            Instant approvedAt) {
        this.roleCode = requireText(roleCode, "roleCode is required.");
        this.dataScope = dataScope == null || dataScope.isBlank() ? "GLOBAL" : dataScope.trim();
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.approved = approved;
        this.approvedBy = approvedBy == null || approvedBy.isBlank() ? null : approvedBy.trim();
        this.approvedAt = approvedAt;
        this.createdAt = LocalDateTime.now();
    }

    static RoleAssignmentJpaEntity from(RoleAssignment assignment) {
        return new RoleAssignmentJpaEntity(
                assignment.roleCode(),
                assignment.dataScope(),
                assignment.validFrom(),
                assignment.validTo(),
                assignment.approved(),
                null,
                assignment.approved() ? Instant.now() : null);
    }

    void assignUser(AuthUserJpaEntity user) {
        this.user = user;
    }

    RoleAssignment toDomain() {
        return new RoleAssignment(roleCode, dataScope, validFrom, validTo, approved);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
