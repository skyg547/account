package com.ho.account.masterdata.core.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 계정과목(Account Subject) 엔티티.
 * SCD2(Slowly Changing Dimension Type 2) 방식을 사용하여 관리한다.
 * 유일한 계정코드(code)가 존재하더라도 연관관계는 대체 키인 기술적인 기본키(id)를 사용한다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 계정과목은 회사에서 돈이 들어오고 나가는 명목(이름표)을 뜻합니다.
 * 예를 들어, "직원 월급"은 '급여'라는 계정과목표를 달고 기록됩니다.
 * 이 클래스는 시스템에서 사용되는 모든 계정과목표의 마스터 데이터를 정의합니다.
 * 특히, 회계 정책이 바뀌어 계정과목의 속성이 변경되더라도 과거 데이터(전표)에 영향을 
 * 주지 않도록 유효기간(validFrom, validTo)을 두어 과거 이력을 모두 보존하는 SCD2 방식을 사용합니다.
 */
@Entity
@Table(name = "account_subjects", indexes = {
    @Index(name = "idx_account_code_valid", columnList = "code, valid_from, valid_to")
})
@Getter
@Setter
@NoArgsConstructor
public class AccountSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String code; // 계정코드 (유일 코드가 아님에 주의)

    @Column(nullable = false, length = 100)
    private String name; // 계정과목명

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private AccountSubject parent; // 상위 계정과목 (ID 기반 연관관계)

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private AccountCategory category;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BalanceType balanceType;

    @Column(length = 100)
    private String reportLine;

    @Column(length = 100)
    private String regulatoryMappingCode;

    @Column(nullable = false)
    private boolean unsettled;

    @Column(nullable = false)
    private LocalDate validFrom;

    @Column(nullable = false)
    private LocalDate validTo;

    @Column(nullable = false)
    private boolean fixedAsset;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    public enum AccountCategory {
        ASSETS, LIABILITIES, EQUITY, REVENUE, EXPENSES
    }

    public enum AccountType {
        ASSETS, LIABILITIES, EQUITY, REVENUE, EXPENSES,
        NON_OPERATING_INCOME, NON_OPERATING_EXPENSES
    }

    public enum BalanceType {
        DEBIT, CREDIT
    }

    /**
     * 기존 코드와의 호환성을 위해 남겨둔 메서드들입니다.
     */
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

    @Deprecated
    public void setUseYn(boolean useYn) {
        // Current model uses validFrom/validTo instead of useYn.
    }

    @Deprecated
    public void setActive(boolean active) {
        // Current model uses validFrom/validTo instead of active flag.
    }

    @Deprecated
    public void setDescription(String description) {
        this.reportLine = description;
    }

    /**
     * 특정 시점에 유효한지 확인
     */
    public boolean isValid(LocalDate date) {
        return (date.isEqual(validFrom) || date.isAfter(validFrom)) && (date.isEqual(validTo) || date.isBefore(validTo));
    }

    /**
     * 현재 이력을 종료
     */
    public void terminate(LocalDate endDate) {
        this.validTo = endDate;
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.auditUser == null) this.auditUser = "SYSTEM";
        if (this.validFrom == null) this.validFrom = LocalDate.now();
        if (this.validTo == null) this.validTo = LocalDate.of(9999, 12, 31);
        if (this.category == null) this.category = AccountCategory.ASSETS;
        if (this.accountType == null) this.accountType = AccountType.valueOf(this.category.name());
        
        if (this.balanceType == null && this.category != null) {
            this.balanceType = (category == AccountCategory.LIABILITIES || category == AccountCategory.EQUITY || category == AccountCategory.REVENUE)
                    ? BalanceType.CREDIT : BalanceType.DEBIT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
