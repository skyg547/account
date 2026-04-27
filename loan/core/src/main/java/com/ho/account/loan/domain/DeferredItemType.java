package com.ho.account.loan.domain;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ?´ì—° ??ª© ? í˜• (Deferred Item Type) ?”í‹°??
 * ?´ì—° ?€?ì´ ?˜ëŠ” ?˜ìˆ˜ë£Œë‚˜ ë¹„ìš© ?±ì˜ ? í˜•???•ì˜?˜ê³ , ê´€???Œê³„ ê³„ì •??ë§¤í•‘?©ë‹ˆ??
 */
@Entity
@Table(name = "deferred_item_types")
public class DeferredItemType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code; // ?´ì—° ??ª© ì½”ë“œ (?? LOAN_ORIGINATION_FEE)

    @Column(nullable = false, length = 200)
    private String name; // ?´ì—° ??ª©ëª?

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DeferralMethod deferralMethod; // ?´ì—° ë°©ë²• (STRAIGHT_LINE, EIR_METHOD ??

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deferred_asset_account_code")
    private AccountSubject deferredAssetAccount; // ?´ì—° ?ì‚° ê³„ì • (?? ?´ì—°?€ì¶œë??€?ìµ ?ì‚°)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recognized_income_account_code")
    private AccountSubject recognizedIncomeAccount; // ?¸ì‹ ?ìµ ê³„ì • (?? ?´ì?˜ìµ)

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    public enum DeferralMethod {
        STRAIGHT_LINE, EIR_METHOD, EFFECTIVE_INTEREST_METHOD // EIR_METHOD?€ EFFECTIVE_INTEREST_METHOD???™ì¼ ê°œë…?´ì?ë§?ëª…ì‹œ?ìœ¼ë¡?êµ¬ë¶„
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

    // Getter ë°?Setter
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
