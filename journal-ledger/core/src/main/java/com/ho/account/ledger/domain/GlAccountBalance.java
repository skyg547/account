package com.ho.account.journalledger.domain.ledger;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.Currency;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 珥앷퀎?뺤썝??(GL) 怨꾩젙 ?붿븸 ?뷀떚??
 * ?뱀젙 怨꾩젙怨쇰ぉ, ?듯솕, ?쇱옄蹂??붿븸??愿由ы빀?덈떎.
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
    private AccountSubject accountSubject; // ?붿븸??愿由ы븷 怨꾩젙怨쇰ぉ

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currency; // ?붿븸 ?듯솕 (湲곗? ?듯솕 ?먮뒗 嫄곕옒 ?듯솕)

    @Column(nullable = false)
    private LocalDate accountingDate; // ?붿븸 湲곗???

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private GlBalanceType balanceType; // ?붿븸 ???(DEBIT, CREDIT)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO; // ?붿븸 湲덉븸

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getter 諛?Setter
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
