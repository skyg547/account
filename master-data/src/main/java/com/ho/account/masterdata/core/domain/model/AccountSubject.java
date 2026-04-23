package com.ho.account.masterdata.core.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ?æ©?ôÊá∞??Account Subject) ????
 * SCD2(Slowly Changing Dimension Type 2)???ºç??çÏæ∂ û¬?∞Î???àÎñé.
 * ??àÏî™???æ©???ÑÎ∂æ?code)??∞Î£Ñ ≤Í≥å???ïÏî™ ??ª‚Ö® ?Ç´??õ¬?????àÎ£Ñ????±—ãÍ∂é(id)???????∏Îï≤??
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
    private String code; // ?æ©???ÑÎ∂æ?(??àÏî™ ?ÑÎ∂æ∂Â™õ? ????∞Íæ©??∞ÎåÅ??õ¬??

    @Column(nullable = false, length = 100)
    private String name; // ?æ©?ôÊá∞?âÔßè?

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private AccountSubject parent; // ?∏Ïêû ?æ©?ôÊá∞??(ID ≤Í≥ï??æ©??¥—ä‚?

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
     * ??âÏüæ ???í™??????óÎøâ???æÎ™Ñ????æ©??????ÜÏì£ ?ãÎúï ?ÑÎ∂æ?????ÖÏÜö??éÎ∂ø??ñÏóØ??àÎñé.
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
     * ?ò± ??ñÏ†è???èÏäö??? ?î§
     */
    public boolean isValid(LocalDate date) {
        return (date.isEqual(validFrom) || date.isAfter(validFrom)) && (date.isEqual(validTo) || date.isBefore(validTo));
    }

    /**
     * ??ÅÏüæ ∞Íæ©???çÎçá??ÇÖ? Ñé??
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
