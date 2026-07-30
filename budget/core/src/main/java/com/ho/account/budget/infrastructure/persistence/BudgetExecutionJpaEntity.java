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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 원천 문서 라인의 집행/취소 증적입니다.
 *
 * <p>예산과의 JPA 연관관계를 두지 않아 집행 목록 조회가 plan 프록시를 행마다 읽는
 * N+1 접근으로 번지지 않으며, 필요한 aggregate는 application service가 명시적으로 잠급니다.</p>
 */
@Entity
@Table(
        name = "budget_executions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_budget_executions_source",
                columnNames = {"source_type", "source_id", "source_line_id"}))
public class BudgetExecutionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "source_id", nullable = false, length = 100)
    private String sourceId;

    @Column(name = "source_line_id", nullable = false, length = 100)
    private String sourceLineId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "execution_date", nullable = false)
    private LocalDate executionDate;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "executed_by", nullable = false, length = 80, updatable = false)
    private String executedBy;

    @Column(name = "executed_at", nullable = false, updatable = false)
    private LocalDateTime executedAt;

    @Column(name = "cancelled_by", length = 80)
    private String cancelledBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    protected BudgetExecutionJpaEntity() {
    }

    @PrePersist
    void onCreate() {
        executedAt = executedAt == null ? LocalDateTime.now() : executedAt;
    }

    Long getId() {
        return id;
    }

    Long getPlanId() {
        return planId;
    }

    void setPlanId(Long planId) {
        this.planId = planId;
    }

    String getSourceType() {
        return sourceType;
    }

    void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    String getSourceId() {
        return sourceId;
    }

    void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    String getSourceLineId() {
        return sourceLineId;
    }

    void setSourceLineId(String sourceLineId) {
        this.sourceLineId = sourceLineId;
    }

    BigDecimal getAmount() {
        return amount;
    }

    void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    LocalDate getExecutionDate() {
        return executionDate;
    }

    void setExecutionDate(LocalDate executionDate) {
        this.executionDate = executionDate;
    }

    String getStatus() {
        return status;
    }

    void setStatus(String status) {
        this.status = status;
    }

    String getExecutedBy() {
        return executedBy;
    }

    void setExecutedBy(String executedBy) {
        this.executedBy = executedBy;
    }

    String getCancelledBy() {
        return cancelledBy;
    }

    void setCancelledBy(String cancelledBy) {
        if (this.cancelledBy == null && cancelledBy != null && cancelledAt == null) {
            cancelledAt = LocalDateTime.now();
        }
        this.cancelledBy = cancelledBy;
    }

    LocalDateTime getExecutedAt() {
        return executedAt;
    }

    LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
}
