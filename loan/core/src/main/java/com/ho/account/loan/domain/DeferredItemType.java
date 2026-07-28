package com.ho.account.loan.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 이연 항목 유형(Deferred Item Type) 엔티티.
 *
 * <p>대출 실행 수수료나 부대비용처럼 한 번에 손익 처리하지 않고 기간에 걸쳐 인식할
 * 항목의 유형과 회계 계정 매핑을 정의합니다.
 */
@Entity
@Table(name = "deferred_item_types")
public class DeferredItemType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code; // 이연 항목 코드 (예: LOAN_ORIGINATION_FEE)

    @Column(nullable = false, length = 200)
    private String name; // 이연 항목명

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DeferralMethod deferralMethod; // 이연 방식 (STRAIGHT_LINE, EIR_METHOD 등)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EirCashFlowTreatment eirCashFlowTreatment = EirCashFlowTreatment.CUSTOMER_FEE_INFLOW;

    @Column(name = "deferred_asset_account_ref", length = 20)
    private String deferredAssetAccountCode; // Master Data 엔티티 대신 계정코드 값만 소유합니다.

    @Column(name = "recognized_income_account_ref", length = 20)
    private String recognizedIncomeAccountCode; // Master Data 엔티티 대신 계정코드 값만 소유합니다.

    @Transient
    private String deferredAssetAccountName;

    @Transient
    private String recognizedIncomeAccountName;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    public enum DeferralMethod {
        STRAIGHT_LINE, EIR_METHOD, EFFECTIVE_INTEREST_METHOD // EIR_METHOD와 EFFECTIVE_INTEREST_METHOD는 같은 개념이지만 명시적으로 구분합니다.
    }

    public enum EirCashFlowTreatment {
        CUSTOMER_FEE_INFLOW,       // 고객이 낸 수수료: 대출자의 순투자액을 줄여 EIR을 높입니다.
        ORIGINATION_COST_OUTFLOW,  // 회사가 부담한 직접 비용: 순투자액을 늘려 EIR을 낮춥니다.
        EXCLUDED_FROM_EIR          // 세금/대납 등 EIR 현금흐름에서 제외할 항목입니다.
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.eirCashFlowTreatment == null) {
            this.eirCashFlowTreatment = EirCashFlowTreatment.CUSTOMER_FEE_INFLOW;
        }
        if (this.auditUser == null)
            this.auditUser = "SYSTEM";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getter and Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public DeferralMethod getDeferralMethod() {
        return deferralMethod;
    }

    public void setDeferralMethod(DeferralMethod deferralMethod) {
        this.deferralMethod = deferralMethod;
    }

    public EirCashFlowTreatment getEirCashFlowTreatment() {
        return eirCashFlowTreatment;
    }

    public void setEirCashFlowTreatment(EirCashFlowTreatment eirCashFlowTreatment) {
        this.eirCashFlowTreatment = eirCashFlowTreatment;
    }

    public String getDeferredAssetAccountCode() {
        return deferredAssetAccountCode;
    }

    public void setDeferredAssetAccountCode(String deferredAssetAccountCode) {
        this.deferredAssetAccountCode = normalizeNullableCode(deferredAssetAccountCode);
    }

    public String getDeferredAssetAccountName() {
        return deferredAssetAccountName;
    }

    public String getRecognizedIncomeAccountCode() {
        return recognizedIncomeAccountCode;
    }

    public void setRecognizedIncomeAccountCode(String recognizedIncomeAccountCode) {
        this.recognizedIncomeAccountCode = normalizeNullableCode(recognizedIncomeAccountCode);
    }

    public String getRecognizedIncomeAccountName() {
        return recognizedIncomeAccountName;
    }

    public void attachAccountDescriptions(String deferredAssetAccountName, String recognizedIncomeAccountName) {
        this.deferredAssetAccountName = normalizeNullableText(deferredAssetAccountName, 100);
        this.recognizedIncomeAccountName = normalizeNullableText(recognizedIncomeAccountName, 100);
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

    public static DeferredItemType create(
            String code,
            String name,
            String description,
            DeferralMethod deferralMethod,
            EirCashFlowTreatment treatment,
            String deferredAssetAccountCode,
            String recognizedIncomeAccountCode,
            boolean active,
            String actor) {
        DeferredItemType type = new DeferredItemType();
        type.code = requireText(code, "code", 100);
        type.name = requireText(name, "name", 200);
        type.description = normalizeNullableText(description, 500);
        type.deferralMethod = java.util.Objects.requireNonNull(deferralMethod, "deferralMethod is required.");
        type.eirCashFlowTreatment = java.util.Objects.requireNonNull(treatment, "eirCashFlowTreatment is required.");
        type.deferredAssetAccountCode = normalizeNullableCode(deferredAssetAccountCode);
        type.recognizedIncomeAccountCode = normalizeNullableCode(recognizedIncomeAccountCode);
        type.isActive = active;
        type.auditUser = requireText(actor, "actor", 50);
        return type;
    }

    private static String normalizeNullableCode(String value) {
        return normalizeNullableText(value, 20);
    }

    private static String normalizeNullableText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException("value must not exceed " + maxLength + " characters.");
        }
        return normalized;
    }

    private static String requireText(String value, String field, int maxLength) {
        String normalized = normalizeNullableText(value, maxLength);
        if (normalized == null) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return normalized;
    }
}
