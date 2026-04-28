package com.ho.account.loan.domain;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Currency;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ?異?(Loan) 怨꾩빟 ?뷀떚??
 * ?異?怨꾩빟??湲곕낯 ?뺣낫瑜?愿由ы븯硫? ?좏슚?댁옄??EIR) 怨꾩궛??湲곗큹媛 ?⑸땲??
 */
@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String loanNumber; // ?異?踰덊샇

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_partner_id", nullable = false)
    private BusinessPartner businessPartner; // 李⑥엯??(borrower)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LoanType loanType; // ?異??좏삎 (TERM_LOAN, REVOLVING_LOAN, MORTGAGE ??

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currency; // ?듯솕

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principalAmount; // ?먭툑

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate; // 紐낅ぉ ?댁옄??(???댁옄??

    @Column(nullable = false)
    private LocalDate disbursalDate; // ?異??ㅽ뻾??

    @Column(nullable = false)
    private LocalDate maturityDate; // 留뚭린??

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentFrequency paymentFrequency; // ?곹솚 二쇨린

    @Column(precision = 5, scale = 4)
    private BigDecimal initialEIR; // 理쒖큹 ?좏슚 ?댁옄??(Effective Interest Rate)

    @Column(precision = 5, scale = 4)
    private BigDecimal currentEIR; // ?꾩옱 ?좏슚 ?댁옄??(?ш퀎????蹂寃?媛??

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoanStatus status; // ?異??곹깭 (ACTIVE, REPAID, DEFAULTED)

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    public enum LoanType {
        TERM_LOAN, REVOLVING_LOAN, MORTGAGE, AUTO_LOAN, PERSONAL_LOAN
    }

    public enum PaymentFrequency {
        DAILY, WEEKLY, BI_WEEKLY, MONTHLY, QUARTERLY, SEMI_ANNUALLY, ANNUALLY
    }

    public enum LoanStatus {
        ACTIVE, REPAID, DEFAULTED, WRITTEN_OFF, CANCELLED
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null)
            this.status = LoanStatus.ACTIVE;
        if (this.auditUser == null)
            this.auditUser = "SYSTEM";
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

    public String getLoanNumber() {
        return loanNumber;
    }

    public void setLoanNumber(String loanNumber) {
        this.loanNumber = loanNumber;
    }

    public BusinessPartner getBusinessPartner() {
        return businessPartner;
    }

    public void setBusinessPartner(BusinessPartner businessPartner) {
        this.businessPartner = businessPartner;
    }

    public LoanType getLoanType() {
        return loanType;
    }

    public void setLoanType(LoanType loanType) {
        this.loanType = loanType;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public LocalDate getDisbursalDate() {
        return disbursalDate;
    }

    public void setDisbursalDate(LocalDate disbursalDate) {
        this.disbursalDate = disbursalDate;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }

    public void setMaturityDate(LocalDate maturityDate) {
        this.maturityDate = maturityDate;
    }

    public PaymentFrequency getPaymentFrequency() {
        return paymentFrequency;
    }

    public void setPaymentFrequency(PaymentFrequency paymentFrequency) {
        this.paymentFrequency = paymentFrequency;
    }

    public BigDecimal getInitialEIR() {
        return initialEIR;
    }

    public void setInitialEIR(BigDecimal initialEIR) {
        this.initialEIR = initialEIR;
    }

    public BigDecimal getCurrentEIR() {
        return currentEIR;
    }

    public void setCurrentEIR(BigDecimal currentEIR) {
        this.currentEIR = currentEIR;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAuditUser() {
        return auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }
}
