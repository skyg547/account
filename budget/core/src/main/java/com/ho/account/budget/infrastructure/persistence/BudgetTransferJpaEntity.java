package com.ho.account.budget.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 전용 요청의 감사 증적을 저장하며 두 예산은 객체 연관관계 대신 scalar FK로 보관합니다. */
@Entity
@Table(
        name = "budget_transfers",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_budget_transfers_request_key",
                columnNames = "request_key"))
public class BudgetTransferJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_key", nullable = false, length = 100)
    private String requestKey;

    @Column(name = "from_plan_id", nullable = false)
    private Long fromPlanId;

    @Column(name = "to_plan_id", nullable = false)
    private Long toPlanId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "requested_by", nullable = false, length = 80, updatable = false)
    private String requestedBy;

    @Column(name = "approved_by", length = 80)
    private String approvedBy;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    protected BudgetTransferJpaEntity() {
    }

    @PrePersist
    void onCreate() {
        requestedAt = requestedAt == null ? LocalDateTime.now() : requestedAt;
    }

    Long getId() {
        return id;
    }

    String getRequestKey() {
        return requestKey;
    }

    void setRequestKey(String requestKey) {
        this.requestKey = requestKey;
    }

    Long getFromPlanId() {
        return fromPlanId;
    }

    void setFromPlanId(Long fromPlanId) {
        this.fromPlanId = fromPlanId;
    }

    Long getToPlanId() {
        return toPlanId;
    }

    void setToPlanId(Long toPlanId) {
        this.toPlanId = toPlanId;
    }

    BigDecimal getAmount() {
        return amount;
    }

    void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    String getStatus() {
        return status;
    }

    void setStatus(String status) {
        this.status = status;
    }

    String getRequestedBy() {
        return requestedBy;
    }

    void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    String getApprovedBy() {
        return approvedBy;
    }

    void setApprovedBy(String approvedBy) {
        if (this.approvedBy == null && approvedBy != null && decidedAt == null) {
            decidedAt = LocalDateTime.now();
        }
        this.approvedBy = approvedBy;
    }

    LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    LocalDateTime getDecidedAt() {
        return decidedAt;
    }
}
