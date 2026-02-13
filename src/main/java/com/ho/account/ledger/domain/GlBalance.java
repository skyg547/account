package com.ho.account.ledger.domain;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.Currency;
import com.ho.account.basic.domain.Department;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 총계정원장 잔액 (GL Balance)
 */
@Entity
@Table(name = "gl_balances", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "account_code", "fiscal_year", "fiscal_period", "dept_code",
                "currency_code" })
})
public class GlBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_code", referencedColumnName = "code", nullable = false)
    private AccountSubject account;

    @Column(nullable = false, length = 4)
    private String fiscalYear;

    @Column(nullable = false, length = 2)
    private String fiscalPeriod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_code", referencedColumnName = "code", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal beginBalanceDr = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal beginBalanceCr = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal currentPeriodDr = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal currentPeriodCr = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal endBalanceDr = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal endBalanceCr = BigDecimal.ZERO;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.auditUser == null)
            this.auditUser = "SYSTEM";
        calculateEndBalance();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        calculateEndBalance();
    }

    public void calculateEndBalance() {
        this.endBalanceDr = this.beginBalanceDr.add(this.currentPeriodDr);
        this.endBalanceCr = this.beginBalanceCr.add(this.currentPeriodCr);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AccountSubject getAccount() {
        return account;
    }

    public void setAccount(AccountSubject account) {
        this.account = account;
    }

    public String getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(String fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public String getFiscalPeriod() {
        return fiscalPeriod;
    }

    public void setFiscalPeriod(String fiscalPeriod) {
        this.fiscalPeriod = fiscalPeriod;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public BigDecimal getBeginBalanceDr() {
        return beginBalanceDr;
    }

    public void setBeginBalanceDr(BigDecimal beginBalanceDr) {
        this.beginBalanceDr = beginBalanceDr;
    }

    public BigDecimal getBeginBalanceCr() {
        return beginBalanceCr;
    }

    public void setBeginBalanceCr(BigDecimal beginBalanceCr) {
        this.beginBalanceCr = beginBalanceCr;
    }

    public BigDecimal getCurrentPeriodDr() {
        return currentPeriodDr;
    }

    public void setCurrentPeriodDr(BigDecimal currentPeriodDr) {
        this.currentPeriodDr = currentPeriodDr;
    }

    public BigDecimal getCurrentPeriodCr() {
        return currentPeriodCr;
    }

    public void setCurrentPeriodCr(BigDecimal currentPeriodCr) {
        this.currentPeriodCr = currentPeriodCr;
    }

    public BigDecimal getEndBalanceDr() {
        return endBalanceDr;
    }

    public void setEndBalanceDr(BigDecimal endBalanceDr) {
        this.endBalanceDr = endBalanceDr;
    }

    public BigDecimal getEndBalanceCr() {
        return endBalanceCr;
    }

    public void setEndBalanceCr(BigDecimal endBalanceCr) {
        this.endBalanceCr = endBalanceCr;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getAuditUser() {
        return auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }
}
