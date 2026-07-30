package com.ho.account.budget.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * {@code budget_plans}의 물리 컬럼만 표현합니다.
 *
 * <p>도메인 객체에 JPA 애노테이션을 넣지 않고 이 저장 모델을 분리하면 예산 규칙은
 * 순수 Java로 유지되고, 낙관적 버전과 감사 시각 같은 저장 기술 책임은 이 경계에 남습니다.</p>
 */
@Entity
@Table(
        name = "budget_plans",
        uniqueConstraints = {
            @UniqueConstraint(name = "uq_budget_plans_plan_code", columnNames = "plan_code"),
            @UniqueConstraint(
                    name = "uq_budget_plans_business_key",
                    columnNames = {"year_month", "department_code", "account_code"})
        })
public class BudgetPlanJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "plan_code", nullable = false, length = 50)
    private String planCode;

    @Column(name = "year_month", nullable = false, length = 6)
    private String yearMonth;

    @Column(name = "department_code", nullable = false, length = 50)
    private String departmentCode;

    @Column(name = "account_code", nullable = false, length = 50)
    private String accountCode;

    @Column(name = "allocated_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal allocatedAmount;

    @Column(name = "transferred_in_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal transferredInAmount;

    @Column(name = "transferred_out_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal transferredOutAmount;

    @Column(name = "executed_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal executedAmount;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_by", nullable = false, length = 80, updatable = false)
    private String createdBy;

    @Column(name = "approved_by", length = 80)
    private String approvedBy;

    @Column(name = "closed_by", length = 80)
    private String closedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "audit_user", nullable = false, length = 80)
    private String auditUser;

    protected BudgetPlanJpaEntity() {
        // JPA가 조회 결과를 복원할 때 사용합니다.
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    Long getId() {
        return id;
    }

    Long getVersion() {
        return version;
    }

    String getPlanCode() {
        return planCode;
    }

    void setPlanCode(String planCode) {
        this.planCode = planCode;
    }

    String getYearMonth() {
        return yearMonth;
    }

    void setYearMonth(String yearMonth) {
        this.yearMonth = yearMonth;
    }

    String getDepartmentCode() {
        return departmentCode;
    }

    void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    String getAccountCode() {
        return accountCode;
    }

    void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    BigDecimal getAllocatedAmount() {
        return allocatedAmount;
    }

    void setAllocatedAmount(BigDecimal allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
    }

    BigDecimal getTransferredInAmount() {
        return transferredInAmount;
    }

    void setTransferredInAmount(BigDecimal transferredInAmount) {
        this.transferredInAmount = transferredInAmount;
    }

    BigDecimal getTransferredOutAmount() {
        return transferredOutAmount;
    }

    void setTransferredOutAmount(BigDecimal transferredOutAmount) {
        this.transferredOutAmount = transferredOutAmount;
    }

    BigDecimal getExecutedAmount() {
        return executedAmount;
    }

    void setExecutedAmount(BigDecimal executedAmount) {
        this.executedAmount = executedAmount;
    }

    String getStatus() {
        return status;
    }

    void setStatus(String status) {
        this.status = status;
    }

    String getCreatedBy() {
        return createdBy;
    }

    void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    String getApprovedBy() {
        return approvedBy;
    }

    void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    String getClosedBy() {
        return closedBy;
    }

    void setClosedBy(String closedBy) {
        this.closedBy = closedBy;
    }

    void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }

    LocalDateTime getCreatedAt() {
        return createdAt;
    }

    LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    String getAuditUser() {
        return auditUser;
    }
}
