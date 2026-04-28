package com.ho.account.loan.domain;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ?댁뿰 ??ぉ ?좏삎 (Deferred Item Type) ?뷀떚??
 * ?댁뿰 ??곸씠 ?섎뒗 ?섏닔猷뚮굹 鍮꾩슜 ?깆쓽 ?좏삎???뺤쓽?섍퀬, 愿???뚭퀎 怨꾩젙??留ㅽ븨?⑸땲??
 */
@Entity
@Table(name = "deferred_item_types")
public class DeferredItemType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code; // ?댁뿰 ??ぉ 肄붾뱶 (?? LOAN_ORIGINATION_FEE)

    @Column(nullable = false, length = 200)
    private String name; // ?댁뿰 ??ぉ紐?

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DeferralMethod deferralMethod; // ?댁뿰 諛⑸쾿 (STRAIGHT_LINE, EIR_METHOD ??

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deferred_asset_account_code")
    private AccountSubject deferredAssetAccount; // ?댁뿰 ?먯궛 怨꾩젙 (?? ?댁뿰?異쒕???먯씡 ?먯궛)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recognized_income_account_code")
    private AccountSubject recognizedIncomeAccount; // ?몄떇 ?먯씡 怨꾩젙 (?? ?댁옄?섏씡)

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    public enum DeferralMethod {
        STRAIGHT_LINE, EIR_METHOD, EFFECTIVE_INTEREST_METHOD // EIR_METHOD? EFFECTIVE_INTEREST_METHOD???숈씪 媛쒕뀗?댁?留?紐낆떆?곸쑝濡?援щ텇
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

    // Getter 諛?Setter
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
