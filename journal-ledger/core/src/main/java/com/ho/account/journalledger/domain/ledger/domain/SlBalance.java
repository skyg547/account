package com.ho.account.journalledger.domain.ledger.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Entity
@Table(name = "sl_balances", uniqueConstraints = {
    @UniqueConstraint(name = "uk_sl_balance_key", columnNames = {"account_code", "bp_code", "dept_code", "currency_code", "balance_date", "period"})
}, indexes = {
    @Index(name = "idx_sl_balance_lookup", columnList = "account_code, bp_code, dept_code, balance_date"),
    @Index(name = "idx_sl_balance_period", columnList = "period, account_code")
})
@Getter
@Setter
@NoArgsConstructor
public class SlBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_code", nullable = false, length = 50)
    private String accountCode;

    @Column(name = "bp_code", length = 50)
    private String businessPartnerCode;

    @Column(name = "dept_code", length = 50)
    private String departmentCode;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "balance_date", nullable = false)
    private LocalDate balanceDate;

    @Column(nullable = false)
    private YearMonth period;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal beginningBalance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal endingBalance = BigDecimal.ZERO;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public void addDebit(BigDecimal amount) {
        this.debitAmount = (this.debitAmount == null ? BigDecimal.ZERO : this.debitAmount).add(amount);
        recalculate();
    }

    public void addCredit(BigDecimal amount) {
        this.creditAmount = (this.creditAmount == null ? BigDecimal.ZERO : this.creditAmount).add(amount);
        recalculate();
    }

    public void recalculate() {
        BigDecimal beg = this.beginningBalance == null ? BigDecimal.ZERO : this.beginningBalance;
        BigDecimal dr  = this.debitAmount      == null ? BigDecimal.ZERO : this.debitAmount;
        BigDecimal cr  = this.creditAmount     == null ? BigDecimal.ZERO : this.creditAmount;
        this.endingBalance = beg.add(dr).subtract(cr);
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.beginningBalance == null) this.beginningBalance = BigDecimal.ZERO;
        if (this.debitAmount      == null) this.debitAmount      = BigDecimal.ZERO;
        if (this.creditAmount     == null) this.creditAmount     = BigDecimal.ZERO;
        recalculate();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}