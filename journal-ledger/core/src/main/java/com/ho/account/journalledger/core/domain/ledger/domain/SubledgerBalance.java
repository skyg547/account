package com.ho.account.journalledger.domain.ledger;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Currency;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * è¹‚ëŒ??ë¨?˜£ (Subledger) ?ë¶¿ë¸¸ ?ë·€???
 * ?ë±€???¨ê¾©?™æ€¨ì‡°?? å«„ê³•?’ï§£? ???†•, ??±ì˜„è¹??ë¶¿ë¸¸???¿Â€?±Ñ‹ë???ˆë–.
 * äºŒì‡°ì¤?èª˜ë©¸ê»??Unsettled) ?¨ê¾©???? ï§ã…¼?§ï§¢ê¾§í…’, ï§ã…¼??§¢ê¾¨Ğ????????????¸ë•²??
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
    private AccountSubject accountSubject; // ?ë¶¿ë¸¸???¿Â€?±Ñ‹ë¸· ?¨ê¾©?™æ€¨ì‡°??(?? ï§ã…¼?§ï§¢ê¾§í…’, ï§ã…¼??§¢ê¾¨Ğ?

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_partner_code", nullable = false)
    private BusinessPartner businessPartner; // ?ë¶¿ë¸¸???¿Â€?±Ñ‹ë¸· å«„ê³•?’ï§£?

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currency; // ?ë¶¿ë¸¸ ???†•

    @Column(nullable = false)
    private LocalDate accountingDate; // ?ë¶¿ë¸¸ æ¹²ê³—???

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private GlBalanceType balanceType; // ?ë¶¿ë¸¸ ????(DEBIT, CREDIT)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO; // ?ë¶¿ë¸¸ æ¹²ë‰ë¸?

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getter è«?Setter
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
