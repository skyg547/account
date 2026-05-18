package com.ho.account.loan.domain;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Currency;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Loan master aggregate.
 * Stores the unified loan model that replaced the old separate contract entity.
 */
@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_number", nullable = false, unique = true, length = 50)
    private String loanNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_partner_id", nullable = false)
    private BusinessPartner businessPartner;

    @Column(name = "LOAN_PRODUCT", length = 100)
    private String loanProduct;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LoanType loanType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currency;

    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "disbursal_date", nullable = false)
    private LocalDate disbursalDate;

    @Column(name = "maturity_date", nullable = false)
    private LocalDate maturityDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentFrequency paymentFrequency;

    @Column(name = "REPAYMENT_METHOD", length = 50)
    private String repaymentMethod;

    @Column(name = "initial_eir", precision = 5, scale = 4)
    private BigDecimal initialEIR;

    @Column(name = "current_eir", precision = 5, scale = 4)
    private BigDecimal currentEIR;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoanStatus status;

    @Column(name = "CURRENT_PRINCIPAL_BALANCE", precision = 19, scale = 2)
    private BigDecimal currentPrincipalBalance;

    @Column(name = "DEFERRED_LOAN_FEE", precision = 19, scale = 2)
    private BigDecimal deferredLoanFee;

    @Column(name = "TOTAL_INTEREST_PAID", precision = 19, scale = 2)
    private BigDecimal totalInterestPaid = BigDecimal.ZERO;

    @Column(name = "TOTAL_PRINCIPAL_PAID", precision = 19, scale = 2)
    private BigDecimal totalPrincipalPaid = BigDecimal.ZERO;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LoanAmortizationScheduleEntry> amortizationSchedule = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "audit_user", length = 50)
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

    public String getLoanProduct() { return loanProduct; }
    public void setLoanProduct(String loanProduct) { this.loanProduct = loanProduct; }

    public String getRepaymentMethod() { return repaymentMethod; }
    public void setRepaymentMethod(String repaymentMethod) { this.repaymentMethod = repaymentMethod; }

    public BigDecimal getCurrentPrincipalBalance() { return currentPrincipalBalance; }
    public void setCurrentPrincipalBalance(BigDecimal currentPrincipalBalance) { this.currentPrincipalBalance = currentPrincipalBalance; }

    public BigDecimal getDeferredLoanFee() { return deferredLoanFee; }
    public void setDeferredLoanFee(BigDecimal deferredLoanFee) { this.deferredLoanFee = deferredLoanFee; }

    public BigDecimal getTotalInterestPaid() { return totalInterestPaid; }
    public void setTotalInterestPaid(BigDecimal totalInterestPaid) { this.totalInterestPaid = totalInterestPaid; }

    public BigDecimal getTotalPrincipalPaid() { return totalPrincipalPaid; }
    public void setTotalPrincipalPaid(BigDecimal totalPrincipalPaid) { this.totalPrincipalPaid = totalPrincipalPaid; }

    public List<LoanAmortizationScheduleEntry> getAmortizationSchedule() { return amortizationSchedule; }
    public void setAmortizationSchedule(List<LoanAmortizationScheduleEntry> amortizationSchedule) { this.amortizationSchedule = amortizationSchedule; }

    public List<LoanAmortizationScheduleEntry> generateAmortizationSchedule(int totalPeriods) {
        validateScheduleInputs(totalPeriods);

        BigDecimal periodicRate = this.interestRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        BigDecimal payment = calculatePeriodicPayment(periodicRate, totalPeriods);

        BigDecimal remainingBalance = principalAmount;
        List<LoanAmortizationScheduleEntry> entries = new ArrayList<>();

        for (int i = 1; i <= totalPeriods; i++) {
            BigDecimal interest = remainingBalance.multiply(periodicRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principal = payment.subtract(interest);

            if (i == totalPeriods) {
                principal = remainingBalance;
                payment = principal.add(interest);
                remainingBalance = BigDecimal.ZERO;
            } else {
                remainingBalance = remainingBalance.subtract(principal);
            }

            LoanAmortizationScheduleEntry entry = new LoanAmortizationScheduleEntry();
            entry.setLoan(this);
            entry.setPeriodNumber(i);
            entry.setPaymentDate(disbursalDate.plusMonths(i));
            entry.setStartingBalance(remainingBalance.add(principal));
            entry.setInterestAmount(interest);
            entry.setPrincipalAmount(principal);
            entry.setScheduledPaymentAmount(payment);
            entry.setEndingBalance(remainingBalance);
            entry.setEntryType("REPAYMENT");

            entries.add(entry);
        }
        this.amortizationSchedule = entries;
        return entries;
    }

    public void addAmortizationEntry(LoanAmortizationScheduleEntry entry) {
        this.amortizationSchedule.add(entry);
        entry.setLoan(this);
    }

    private void validateScheduleInputs(int totalPeriods) {
        if (totalPeriods <= 0) {
            throw new IllegalArgumentException("totalPeriods must be positive");
        }
        if (principalAmount == null || principalAmount.signum() <= 0) {
            throw new IllegalStateException("principalAmount must be positive");
        }
        if (interestRate == null || interestRate.signum() < 0) {
            throw new IllegalStateException("interestRate must not be negative");
        }
        if (disbursalDate == null) {
            throw new IllegalStateException("disbursalDate is required");
        }
    }

    private BigDecimal calculatePeriodicPayment(BigDecimal periodicRate, int totalPeriods) {
        if (periodicRate.signum() == 0) {
            return principalAmount.divide(BigDecimal.valueOf(totalPeriods), 2, RoundingMode.HALF_UP);
        }

        BigDecimal onePlusRPowerN = periodicRate.add(BigDecimal.ONE).pow(totalPeriods);
        return principalAmount.multiply(periodicRate).multiply(onePlusRPowerN)
                .divide(onePlusRPowerN.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
    }
}
