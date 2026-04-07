package com.ho.account.basic.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime; // LocalDateTime 추가

/**
 * 계정과목(Chart of Accounts) 마스터 엔티티.
 * 계층 구조, 유효 기간, 재무제표 매핑 등을 포함하여 전체 회계 시스템의 기반을 정의합니다.
 */
/**
 * 계정과목(COA) 엔티티
 * 회계 처리의 기본 단위가 되는 계정과목을 관리하며, Hierarchical 구조와 SCD2(Slowly Changing Dimension
 * Type 2)를 지원함.
 */
@Entity
@Table(name = "account_subjects")
public class AccountSubject {

    /**
     * 계정 코드 (Primary Key)
     */
    @Id
    @Column(length = 20)
    private String code;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일시

    @Column(nullable = false)
    private LocalDateTime updatedAt; // 수정일시

    @Column(length = 50)
    private String auditUser; // 감사 사용자

    @Column(nullable = false, length = 100)
    private String name; // 계정과목명

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_code", referencedColumnName = "code")
    private AccountSubject parent; // 상위 계정과목 (계층 구조)

    /**
     * 계정 상위 분류 (자산, 부채, 자본, 수익, 비용)
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private AccountCategory category;

    /**
     * 계정 대분류 (BS, IS, CF 등) - 기존 AccountType 유지 또는 Category와 매핑
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private AccountType accountType;

    /**
     * 계정 잔액 타입 (차변/대변)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BalanceType balanceType;

    /**
     * 재무제표 표시 라인 (재무상태표, 손익계산서 등의 항목)
     */
    @Column(length = 100)
    private String reportLine;

    /**
     * 감독회계 보고서 매핑 코드
     */
    @Column(length = 100)
    private String regulatoryMappingCode;

    /**
     * 미결(채권/채무) 관리 여부
     */
    @Column(nullable = false)
    private boolean unsettled;

    /**
     * 유효 시작일 (SCD2)
     */
    @Column(nullable = false)
    private LocalDate validFrom;

    /**
     * 유효 종료일 (SCD2)
     */
    @Column(nullable = false)
    private LocalDate validTo;

    /**
     * 고정자산 계정 여부
     */
    @Column(nullable = false)
    private boolean fixedAsset;

    // --- Enum Definitions ---
    public enum AccountCategory {
        ASSETS, LIABILITIES, EQUITY, REVENUE, EXPENSES
    }

    public enum AccountType {
        ASSETS, LIABILITIES, EQUITY, REVENUE, EXPENSES,
        NON_OPERATING_INCOME, NON_OPERATING_EXPENSES // 상세 분류가 필요할 수 있음
    }

    public enum BalanceType {
        DEBIT, CREDIT
    }

    // --- Getter 및 Setter ---

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

    public AccountSubject getParent() {
        return parent;
    }

    public void setParent(AccountSubject parent) {
        this.parent = parent;
    }

    public AccountCategory getCategory() {
        return category;
    }

    public void setCategory(AccountCategory category) {
        this.category = category;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    @Deprecated
    public void setAccountType(String accountType) {
        if (accountType == null || accountType.isBlank()) {
            this.accountType = null;
            return;
        }

        String normalized = switch (accountType) {
            case "ASSET" -> "ASSETS";
            case "LIABILITY" -> "LIABILITIES";
            case "EXPENSE" -> "EXPENSES";
            default -> accountType;
        };
        this.accountType = AccountType.valueOf(normalized);
    }

    public BalanceType getBalanceType() {
        return balanceType;
    }

    public void setBalanceType(BalanceType balanceType) {
        this.balanceType = balanceType;
    }

    public String getReportLine() {
        return reportLine;
    }

    public void setReportLine(String reportLine) {
        this.reportLine = reportLine;
    }

    public String getRegulatoryMappingCode() {
        return regulatoryMappingCode;
    }

    public void setRegulatoryMappingCode(String regulatoryMappingCode) {
        this.regulatoryMappingCode = regulatoryMappingCode;
    }

    public boolean isUnsettled() {
        return unsettled;
    }

    public void setUnsettled(boolean unsettled) {
        this.unsettled = unsettled;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    public boolean isFixedAsset() {
        return fixedAsset;
    }

    public void setFixedAsset(boolean fixedAsset) {
        this.fixedAsset = fixedAsset;
    }

    @Deprecated
    public void setUseYn(boolean useYn) {
        // Compatibility shim for legacy tests. Current model does not track useYn.
    }

    @Deprecated
    public void setActive(boolean active) {
        // Compatibility shim for legacy tests. Current model uses validity dates instead.
    }

    @Deprecated
    public void setDescription(String description) {
        this.reportLine = description;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.auditUser == null) {
            this.auditUser = "SYSTEM";
        }
        if (this.category == null) {
            this.category = AccountCategory.ASSETS;
        }
        if (this.accountType == null) {
            this.accountType = AccountType.valueOf(this.category.name());
        }
        if (this.balanceType == null) {
            this.balanceType = this.category == AccountCategory.LIABILITIES
                    || this.category == AccountCategory.EQUITY
                    || this.category == AccountCategory.REVENUE
                    ? BalanceType.CREDIT
                    : BalanceType.DEBIT;
        }
        if (this.validFrom == null) {
            this.validFrom = LocalDate.now();
        }
        if (this.validTo == null) {
            this.validTo = LocalDate.of(9999, 12, 31);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
