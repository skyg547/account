package com.ho.account.journalledger.domain.ledger;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Currency;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 蹂댁“?먯옣 (Subledger) ?붿븸 ?뷀떚??
 * ?뱀젙 怨꾩젙怨쇰ぉ, 嫄곕옒泥? ?듯솕, ?쇱옄蹂??붿븸??愿由ы빀?덈떎.
 * 二쇰줈 誘멸껐??Unsettled) 怨꾩젙(?? 留ㅼ텧梨꾧텒, 留ㅼ엯梨꾨Т)??????ъ슜?⑸땲??
 */
@Entity
@Table(name = "subledger_balances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"account_code", "business_partner_code", "currency_code", "accounting_date", "balance_type"}))
public class SubledgerBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_code", nullable = false)
    private AccountSubject accountSubject; // ?붿븸??愿由ы븷 怨꾩젙怨쇰ぉ (?? 留ㅼ텧梨꾧텒, 留ㅼ엯梨꾨Т)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_partner_code", nullable = false)
    private BusinessPartner businessPartner; // ?붿븸??愿由ы븷 嫄곕옒泥?

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currency; // ?붿븸 ?듯솕

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

    public BusinessPartner getBusinessPartner() {
        return businessPartner;
    }

    public void setBusinessPartner(BusinessPartner businessPartner) {
        this.businessPartner = businessPartner;
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
