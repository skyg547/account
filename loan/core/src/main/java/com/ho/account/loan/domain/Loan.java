package com.ho.account.loan.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Loan master aggregate.
 * Stores the unified loan model that replaced the old separate contract entity.
 */
/**
 * 대출(Loan) 엔티티 — 대출 계약 정보와 잔액, 스케줄을 관리하는 핵심 모델.
 *
 * 🐣 [초보자를 위한 설명]
 * 대출은 '은행이 고객에게 돈을 빌려준 사건' 그 자체를 의미합니다.
 * 이 클래스는 고객이 얼마를 빌렸는지(원금), 이자는 몇 퍼센트인지(이자율), 
 * 그리고 매달 언제 얼마씩 갚아야 하는지(상환 스케줄)를 모두 기억하고 있습니다.
 * 특히 '유효이자율(EIR)'이라는 마법의 숫자를 계산해서, 실제 벌어들이는 수익을 아주 정밀하게 계산해내는 역할을 합니다.
 */
@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_number", nullable = false, unique = true, length = 50)
    private String loanNumber;

    @Column(name = "business_partner_id", nullable = false)
    private Long businessPartnerId;

    @Transient
    private String businessPartnerName;

    @Column(name = "LOAN_PRODUCT", length = 100)
    private String loanProduct;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LoanType loanType;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

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

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

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
        PENDING_DISBURSEMENT, ACTIVE, REPAID, DEFAULTED, WRITTEN_OFF, CANCELLED
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = LoanStatus.PENDING_DISBURSEMENT;
        }
        if (this.currentPrincipalBalance == null) {
            this.currentPrincipalBalance = this.principalAmount;
        }
        if (this.totalInterestPaid == null) {
            this.totalInterestPaid = BigDecimal.ZERO;
        }
        if (this.totalPrincipalPaid == null) {
            this.totalPrincipalPaid = BigDecimal.ZERO;
        }
        this.auditUser = requireActor(this.auditUser == null ? "SYSTEM" : this.auditUser);
        validateContractTerms();
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

    public Long getBusinessPartnerId() {
        return businessPartnerId;
    }

    public void setBusinessPartnerId(Long businessPartnerId) {
        this.businessPartnerId = businessPartnerId;
    }

    public String getBusinessPartnerName() {
        return businessPartnerName;
    }

    public void attachBusinessPartnerName(String businessPartnerName) {
        this.businessPartnerName = requireText(businessPartnerName, "businessPartnerName", 100);
    }

    public LoanType getLoanType() {
        return loanType;
    }

    public void setLoanType(LoanType loanType) {
        this.loanType = loanType;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = normalizeCurrencyCode(currencyCode);
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

    public BigDecimal getOutstandingPrincipal() {
        return currentPrincipalBalance == null ? principalAmount : currentPrincipalBalance;
    }

    public BigDecimal getDeferredLoanFee() { return deferredLoanFee; }
    public void setDeferredLoanFee(BigDecimal deferredLoanFee) { this.deferredLoanFee = deferredLoanFee; }

    public BigDecimal getTotalInterestPaid() { return totalInterestPaid; }
    public void setTotalInterestPaid(BigDecimal totalInterestPaid) { this.totalInterestPaid = totalInterestPaid; }

    public BigDecimal getTotalPrincipalPaid() { return totalPrincipalPaid; }
    public void setTotalPrincipalPaid(BigDecimal totalPrincipalPaid) { this.totalPrincipalPaid = totalPrincipalPaid; }

    public long getLockVersion() {
        return lockVersion;
    }

    public static Loan create(
            String loanNumber,
            Long businessPartnerId,
            String currencyCode,
            LoanType loanType,
            BigDecimal principalAmount,
            BigDecimal interestRate,
            LocalDate disbursalDate,
            LocalDate maturityDate,
            PaymentFrequency paymentFrequency,
            String actor) {
        Loan loan = new Loan();
        loan.loanNumber = requireText(loanNumber, "loanNumber", 50);
        loan.businessPartnerId = requirePositiveId(businessPartnerId, "businessPartnerId");
        loan.currencyCode = normalizeCurrencyCode(currencyCode);
        loan.loanType = java.util.Objects.requireNonNull(loanType, "loanType is required.");
        loan.principalAmount = requirePositiveAmount(principalAmount, "principalAmount");
        loan.interestRate = requireRate(interestRate, "interestRate");
        loan.disbursalDate = java.util.Objects.requireNonNull(disbursalDate, "disbursalDate is required.");
        loan.maturityDate = java.util.Objects.requireNonNull(maturityDate, "maturityDate is required.");
        loan.paymentFrequency = java.util.Objects.requireNonNull(paymentFrequency, "paymentFrequency is required.");
        loan.initialEIR = loan.interestRate;
        loan.currentEIR = loan.interestRate;
        loan.currentPrincipalBalance = loan.principalAmount;
        loan.status = LoanStatus.PENDING_DISBURSEMENT;
        loan.auditUser = requireActor(actor);
        loan.validateContractTerms();
        return loan;
    }

    public void prepareForCreation(String actor) {
        validateContractTerms();
        initialEIR = interestRate;
        currentEIR = interestRate;
        currentPrincipalBalance = principalAmount;
        status = LoanStatus.PENDING_DISBURSEMENT;
        auditUser = requireActor(actor);
    }

    public void activateAfterDisbursal(LocalDate actualDate, BigDecimal amount, String actor) {
        if (status != LoanStatus.PENDING_DISBURSEMENT) {
            throw new IllegalStateException("Only a pending loan can be disbursed: " + status);
        }
        // Legacy pending contracts must fail before LoanService posts the disbursal journal.
        validateSupportedPaymentFrequency();
        if (actualDate == null || actualDate.isBefore(disbursalDate) || actualDate.isAfter(maturityDate)) {
            throw new IllegalArgumentException("disbursalDate must be within the loan contract period.");
        }
        BigDecimal normalizedAmount = requirePositiveAmount(amount, "disbursedAmount");
        if (normalizedAmount.compareTo(principalAmount) != 0) {
            throw new IllegalArgumentException(
                    "This loan model supports one full disbursal equal to principalAmount.");
        }
        status = LoanStatus.ACTIVE;
        currentPrincipalBalance = principalAmount;
        auditUser = requireActor(actor);
    }

    public void applyRecalculatedTerms(
            BigDecimal newOutstandingPrincipal,
            LocalDate newMaturityDate,
            BigDecimal newEir,
            LocalDate recalculationDate,
            String actor) {
        requireActive("recalculate");
        if (recalculationDate == null
                || recalculationDate.isBefore(disbursalDate)
                || recalculationDate.isAfter(maturityDate)) {
            throw new IllegalArgumentException("recalculationDate must be within the current loan period.");
        }
        BigDecimal outstanding = requirePositiveAmount(newOutstandingPrincipal, "newOutstandingPrincipal");
        if (outstanding.compareTo(getOutstandingPrincipal()) > 0) {
            throw new IllegalArgumentException("newOutstandingPrincipal cannot increase the outstanding balance.");
        }
        LocalDate maturity = java.util.Objects.requireNonNull(newMaturityDate, "newMaturityDate is required.");
        if (!maturity.isAfter(recalculationDate)) {
            throw new IllegalArgumentException("newMaturityDate must be after recalculationDate.");
        }
        currentPrincipalBalance = outstanding;
        maturityDate = maturity;
        currentEIR = requireRate(newEir, "newEIR");
        auditUser = requireActor(actor);
    }

    /** Apply contractual payment without recalculating EIR or replacing future schedules. */
    public void applyScheduledRepayment(EIRAmortizationSchedule schedule, String actor) {
        validateScheduledRepayment(schedule);
        String normalizedActor = requireActor(actor);
        BigDecimal principalPaid = totalPrincipalPaid == null ? BigDecimal.ZERO : totalPrincipalPaid;
        BigDecimal interestPaid = totalInterestPaid == null ? BigDecimal.ZERO : totalInterestPaid;
        currentPrincipalBalance = schedule.getEndingBalance();
        totalPrincipalPaid = principalPaid.add(schedule.getPrincipalRepayment());
        totalInterestPaid = interestPaid.add(schedule.getInterestIncome());
        if (currentPrincipalBalance.signum() == 0) {
            status = LoanStatus.REPAID;
        }
        auditUser = normalizedActor;
    }

    public void validateScheduledRepayment(EIRAmortizationSchedule schedule) {
        requireActive("repay on schedule");
        if (schedule == null || schedule.getLoan() == null || id == null
                || !id.equals(schedule.getLoan().getId())) {
            throw new IllegalArgumentException("Schedule must belong to this persisted loan.");
        }
        LocalDate date = schedule.getScheduleDate();
        if (date == null || date.isBefore(disbursalDate) || date.isAfter(maturityDate)) {
            throw new IllegalArgumentException("Repayment date must be within the contract period.");
        }
        BigDecimal principal = requirePositiveAmount(schedule.getPrincipalRepayment(), "principalRepayment");
        BigDecimal interest = schedule.getInterestIncome();
        BigDecimal beginning = schedule.getBeginningBalance();
        BigDecimal ending = schedule.getEndingBalance();
        BigDecimal cash = schedule.getCashFlow();
        if (interest == null || interest.signum() < 0 || beginning == null || ending == null || cash == null
                || beginning.compareTo(getOutstandingPrincipal()) != 0
                || principal.compareTo(beginning) > 0 || ending.signum() < 0
                || beginning.subtract(principal).compareTo(ending) != 0
                || principal.add(interest).compareTo(cash) != 0) {
            throw new IllegalArgumentException("Repayment schedule amounts do not reconcile with the loan balance.");
        }
        if (schedule.getDeferredItemAmortization() != null
                && schedule.getDeferredItemAmortization().signum() != 0) {
            throw new IllegalArgumentException("Deferred amortization requires a separate settlement workflow.");
        }
        CurrencyRoundingPolicy rounding = CurrencyRoundingPolicy.of(currencyCode);
        for (BigDecimal amount : new BigDecimal[] {principal, interest, beginning, ending, cash}) {
            if (rounding.applyRounding(amount).compareTo(amount) != 0) {
                throw new IllegalArgumentException("Scheduled payment must already satisfy currency precision.");
            }
        }
    }

    public void markDefaulted(String actor) {
        requireActive("mark defaulted");
        status = LoanStatus.DEFAULTED;
        auditUser = requireActor(actor);
    }

    public void recoverFromDefault(String actor) {
        if (status != LoanStatus.DEFAULTED) {
            throw new IllegalStateException("Only a defaulted loan can recover: " + status);
        }
        status = LoanStatus.ACTIVE;
        auditUser = requireActor(actor);
    }

    public void updateCurrentEir(BigDecimal annualEir, String actor) {
        requireActive("update EIR");
        currentEIR = requireRate(annualEir, "annualEIR");
        auditUser = requireActor(actor);
    }

    private void validateContractTerms() {
        loanNumber = requireText(loanNumber, "loanNumber", 50);
        businessPartnerId = requirePositiveId(businessPartnerId, "businessPartnerId");
        currencyCode = normalizeCurrencyCode(currencyCode);
        java.util.Objects.requireNonNull(loanType, "loanType is required.");
        principalAmount = requirePositiveAmount(principalAmount, "principalAmount");
        interestRate = requireRate(interestRate, "interestRate");
        java.util.Objects.requireNonNull(disbursalDate, "disbursalDate is required.");
        java.util.Objects.requireNonNull(maturityDate, "maturityDate is required.");
        validateSupportedPaymentFrequency();
        if (!maturityDate.isAfter(disbursalDate)) {
            throw new IllegalArgumentException("maturityDate must be after disbursalDate.");
        }
    }

    private void validateSupportedPaymentFrequency() {
        java.util.Objects.requireNonNull(paymentFrequency, "paymentFrequency is required.");
        // The current EIR schedule uses monthly dates and annualEIR / 12.
        if (paymentFrequency != PaymentFrequency.MONTHLY) {
            throw new IllegalArgumentException("paymentFrequency " + paymentFrequency
                    + " is not supported; only MONTHLY is supported.");
        }
    }

    private void requireActive(String action) {
        if (status != LoanStatus.ACTIVE) {
            throw new IllegalStateException("Only an active loan can " + action + ": " + status);
        }
    }

    private static Long requirePositiveId(Long value, String field) {
        if (value == null || value < 1) {
            throw new IllegalArgumentException(field + " must be positive.");
        }
        return value;
    }

    private static BigDecimal requirePositiveAmount(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(field + " must be positive.");
        }
        return value;
    }

    private static BigDecimal requireRate(BigDecimal value, String field) {
        if (value == null || value.signum() < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(field + " must be a decimal rate between 0 and 1.");
        }
        return value;
    }

    private static String normalizeCurrencyCode(String value) {
        String normalized = requireText(value, "currencyCode", 3).toUpperCase(Locale.ROOT);
        if (normalized.length() != 3) {
            throw new IllegalArgumentException("currencyCode must be a 3-letter ISO code.");
        }
        return normalized;
    }

    private static String requireActor(String actor) {
        return requireText(actor, "actor", 50);
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " must not exceed " + maxLength + " characters.");
        }
        return normalized;
    }
}
