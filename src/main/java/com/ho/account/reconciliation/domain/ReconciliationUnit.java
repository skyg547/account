package com.ho.account.reconciliation.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 대사 단위(Reconciliation Unit) 엔티티
 * 어떤 항목들을 어떤 기준으로 대사할 것인지 정의합니다. (예: 은행계좌 대사, 전표-원장 대사)
 */
@Entity
@Table(name = "reconciliation_units")
public class ReconciliationUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String name; // 대사 단위명 (예: "은행계좌 대사", "전표-원장 대사")

    @Column(length = 500)
    private String description; // 대사 단위 상세 설명

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReconciliationFrequency frequency; // 대사 주기 (DAILY, MONTHLY, etc.)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReconciliationType reconciliationType; // 대사 유형 (BANK_BOOK, GL_SUBLEDGER 등)

    // 대사 기준 (예: 일자, 상품, 통화, 법인)을 JSON 문자열로 저장하여 유연성 확보
    @Column(columnDefinition = "TEXT")
    private String criteriaJson;

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
    public enum ReconciliationFrequency {
        DAILY, WEEKLY, MONTHLY, QUARTERLY, ANNUALLY, ADHOC
    }

    public enum ReconciliationType {
        BANK_BOOK, // 은행계좌 대사 (통장 vs 장부)
        GL_SUBLEDGER, // 총계정원장 vs 보조원장 (전표-원장 대사 포함)
        SOURCE_GL, // 원천 데이터 vs 총계정원장
        INTERCOMPANY, // 계열사 간 대사
        AR_AP_RECEIPT // 채권/채무 vs 수금/지급
    }

    // --- Getters and Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ReconciliationFrequency getFrequency() {
        return frequency;
    }

    public void setFrequency(ReconciliationFrequency frequency) {
        this.frequency = frequency;
    }

    public ReconciliationType getReconciliationType() {
        return reconciliationType;
    }

    public void setReconciliationType(ReconciliationType reconciliationType) {
        this.reconciliationType = reconciliationType;
    }

    public String getCriteriaJson() {
        return criteriaJson;
    }

    public void setCriteriaJson(String criteriaJson) {
        this.criteriaJson = criteriaJson;
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
