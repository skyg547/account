package com.ho.account.loan.domain;

import com.ho.account.basic.domain.AccountSubject;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 이연 항목 유형 (Deferred Item Type) 엔티티
 * 이연 대상이 되는 수수료나 비용 등의 유형을 정의하고, 관련 회계 계정을 매핑합니다.
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
    private DeferralMethod deferralMethod; // 이연 방법 (STRAIGHT_LINE, EIR_METHOD 등)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deferred_asset_account_code")
    private AccountSubject deferredAssetAccount; // 이연 자산 계정 (예: 이연대출부대손익 자산)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recognized_income_account_code")
    private AccountSubject recognizedIncomeAccount; // 인식 손익 계정 (예: 이자수익)

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    public enum DeferralMethod {
        STRAIGHT_LINE, EIR_METHOD, EFFECTIVE_INTEREST_METHOD // EIR_METHOD와 EFFECTIVE_INTEREST_METHOD는 동일 개념이지만 명시적으로 구분
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.auditUser == null)
            this.auditUser = "SYSTEM";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getter 및 Setter
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

    public AccountSubject getDeferredAssetAccount() {
        return deferredAssetAccount;
    }

    public void setDeferredAssetAccount(AccountSubject deferredAssetAccount) {
        this.deferredAssetAccount = deferredAssetAccount;
    }

    public AccountSubject getRecognizedIncomeAccount() {
        return recognizedIncomeAccount;
    }

    public void setRecognizedIncomeAccount(AccountSubject recognizedIncomeAccount) {
        this.recognizedIncomeAccount = recognizedIncomeAccount;
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
