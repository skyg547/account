package com.ho.account.journalledger.domain.ledger;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Department;
import com.ho.account.basic.domain.Currency; // ?꾨씫??Currency import 異붽?
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.LocalDateTime;
import java.util.List; // ?꾨씫??List import 異붽?

/**
 * 蹂댁“?먯옣 ?붿븸 (Subsidiary Ledger Balance) ?뷀떚??
 * ?뱀젙 怨꾩젙怨쇰ぉ, 嫄곕옒泥? 遺??議고빀???쇰퀎 諛??붾퀎 ?붿븸 ?뺣낫瑜?湲곕줉?⑸땲??
 */
@Entity
@Table(name = "sl_balances", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"account_subject_id", "business_partner_id", "department_id", "currency_code", "balance_date", "period"})
}, indexes = {
    @Index(name = "idx_sl_balance_account_bp_date", columnList = "account_subject_id, business_partner_id, balanceDate")
})
public class SlBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_subject_id", nullable = false)
    private AccountSubject accountSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_partner_id")
    private BusinessPartner businessPartner; // 嫄곕옒泥?(?좏깮 ?ы빆)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department; // 洹?띾???(?좏깮 ?ы빆)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currency;

    @Column(nullable = false)
    private LocalDate balanceDate; // ?붿븸 ?쇱옄 (?쇰퀎 ?붿븸)

    @Column(nullable = false)
    private YearMonth period; // ?뚭퀎 湲곌컙 (?붾퀎 ?붿븸 吏묎퀎瑜??꾪븳)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal beginningBalance = BigDecimal.ZERO; // 湲곗큹 ?붿븸

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal debitAmount = BigDecimal.ZERO; // 李⑤? 諛쒖깮??

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal creditAmount = BigDecimal.ZERO; // ?蹂 諛쒖깮??

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal endingBalance = BigDecimal.ZERO; // 湲곕쭚 ?붿븸

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public LocalDate getBalanceDate() {
        return balanceDate;
    }

    public void setBalanceDate(LocalDate balanceDate) {
        this.balanceDate = balanceDate;
    }

    public YearMonth getPeriod() {
        return period;
    }

    public void setPeriod(YearMonth period) {
        this.period = period;
    }

    public BigDecimal getBeginningBalance() {
        return beginningBalance;
    }

    public void setBeginningBalance(BigDecimal beginningBalance) {
        this.beginningBalance = beginningBalance;
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = debitAmount;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = creditAmount;
    }

    public BigDecimal getEndingBalance() {
        return endingBalance;
    }

    public void setEndingBalance(BigDecimal endingBalance) {
        this.endingBalance = endingBalance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }
}
