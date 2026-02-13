package com.ho.account.loan.domain;

import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Currency;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 대출 (Loan) 계약 엔티티
 * 대출 계약의 기본 정보를 관리하며, 유효이자율(EIR) 계산의 기초가 됩니다.
 */
@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String loanNumber; // 대출 번호

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_partner_id", nullable = false)
    private BusinessPartner businessPartner; // 차입자 (borrower)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LoanType loanType; // 대출 유형 (TERM_LOAN, REVOLVING_LOAN, MORTGAGE 등)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currency; // 통화

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principalAmount; // 원금

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate; // 명목 이자율 (연 이자율)

    @Column(nullable = false)
    private LocalDate disbursalDate; // 대출 실행일

    @Column(nullable = false)
    private LocalDate maturityDate; // 만기일

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentFrequency paymentFrequency; // 상환 주기

    @Column(precision = 5, scale = 4)
    private BigDecimal initialEIR; // 최초 유효 이자율 (Effective Interest Rate)

    @Column(precision = 5, scale = 4)
    private BigDecimal currentEIR; // 현재 유효 이자율 (재계산 시 변경 가능)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoanStatus status; // 대출 상태 (ACTIVE, REPAID, DEFAULTED)

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

    // Getters and Setters
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
