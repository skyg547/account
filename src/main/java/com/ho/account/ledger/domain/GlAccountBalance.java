package com.ho.account.ledger.domain;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.Currency;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 총계정원장 (GL) 계정 잔액 엔티티.
 * 특정 계정과목, 통화, 일자별 잔액을 관리합니다.
 */
@Entity
@Table(name = "gl_account_balances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"account_code", "currency_code", "accounting_date", "balance_type"}))
public class GlAccountBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_code", nullable = false)
    private AccountSubject accountSubject; // 잔액을 관리할 계정과목

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currency; // 잔액 통화 (기준 통화 또는 거래 통화)

    @Column(nullable = false)
    private LocalDate accountingDate; // 잔액 기준일

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private GlBalanceType balanceType; // 잔액 타입 (DEBIT, CREDIT)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO; // 잔액 금액

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AccountSubject getAccountSubject() {
        return accountSubject;
    }

    public void setAccountSubject(AccountSubject accountSubject) {
        this.accountSubject = accountSubject;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public LocalDate getAccountingDate() {
        return accountingDate;
    }

    public void setAccountingDate(LocalDate accountingDate) {
        this.accountingDate = accountingDate;
    }

    public GlBalanceType getBalanceType() {
        return balanceType;
    }

    public void setBalanceType(GlBalanceType balanceType) {
        this.balanceType = balanceType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
