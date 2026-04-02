package com.ho.account.reconciliation.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 대사 규칙(Reconciliation Rule) 엔티티
 * 자동 매칭을 위한 규칙과 허용 오차를 정의합니다.
 */
@Entity
@Table(name = "reconciliation_rules")
public class ReconciliationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_unit_id", nullable = false)
    private ReconciliationUnit reconciliationUnit;

    @Column(nullable = false, length = 100)
    private String name; // 규칙명 (예: "전표번호 일치", "금액 및 날짜 근접 매칭")

    // 규칙 정의 (매칭 필드, 조건, 집계 방식 등을 JSON 문자열로 저장하여 유연성 확보)
    @Column(columnDefinition = "TEXT")
    private String ruleDefinitionJson;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ToleranceType toleranceType; // 허용 오차 유형 (ABSOLUTE, PERCENTAGE)

    @Column(precision = 19, scale = 8)
    private BigDecimal toleranceValue; // 허용 오차 값

    @Column(nullable = false)
    private Integer priority; // 규칙 적용 우선순위 (낮은 숫자일수록 우선)

    @Column(nullable = false)
    private boolean isActive = true; // 활성화 여부

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.auditUser == null) {
            this.auditUser = "SYSTEM";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Enums ---
    public enum ToleranceType {
        NONE, ABSOLUTE, PERCENTAGE
    }

    // --- Getter 및 Setter ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ReconciliationUnit getReconciliationUnit() {
        return reconciliationUnit;
    }

    public void setReconciliationUnit(ReconciliationUnit reconciliationUnit) {
        this.reconciliationUnit = reconciliationUnit;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRuleDefinitionJson() {
        return ruleDefinitionJson;
    }

    public void setRuleDefinitionJson(String ruleDefinitionJson) {
        this.ruleDefinitionJson = ruleDefinitionJson;
    }

    public ToleranceType getToleranceType() {
        return toleranceType;
    }

    public void setToleranceType(ToleranceType toleranceType) {
        this.toleranceType = toleranceType;
    }

    public BigDecimal getToleranceValue() {
        return toleranceValue;
    }

    public void setToleranceValue(BigDecimal toleranceValue) {
        this.toleranceValue = toleranceValue;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAuditUser() {
        return auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }
}
