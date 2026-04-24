package com.ho.account.journalledger.domain.ledger.domain;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.Currency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * 총계정원장 잔액 (General Ledger Balance)
 * 특정 계정과목 및 통화별 일별/월별 잔액을 관리합니다.
 */
@Entity
@Table(name = "gl_balances", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"account_id", "currency_code", "balance_date", "period"})
}, indexes = {
    @Index(name = "idx_gl_balance_date", columnList = "balance_date")
})
@Getter
@Setter
@NoArgsConstructor
public class GlBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountSubject accountSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currency;

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

    // --- 비즈니스 로직 ---

    public void addDebit(BigDecimal amount) {
        this.debitAmount = (this.debitAmount == null ? BigDecimal.ZERO : this.debitAmount).add(amount);
        recalculate();
    }

    public void addCredit(BigDecimal amount) {
        this.creditAmount = (this.creditAmount == null ? BigDecimal.ZERO : this.creditAmount).add(amount);
        recalculate();
    }

    public void recalculate() {
        // 잔액 = 기초 + 차변 - 대변
        BigDecimal beg = this.beginningBalance == null ? BigDecimal.ZERO : this.beginningBalance;
        BigDecimal dr = this.debitAmount == null ? BigDecimal.ZERO : this.debitAmount;
        BigDecimal cr = this.creditAmount == null ? BigDecimal.ZERO : this.creditAmount;
        this.endingBalance = beg.add(dr).subtract(cr);
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.beginningBalance == null) this.beginningBalance = BigDecimal.ZERO;
        if (this.debitAmount == null) this.debitAmount = BigDecimal.ZERO;
        if (this.creditAmount == null) this.creditAmount = BigDecimal.ZERO;
        recalculate();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
