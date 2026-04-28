package com.ho.account.loan.domain;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList; // ArrayList import
import java.util.List; // List import

@Entity
@Table(name = "LOAN_CONTRACTS")
public class LoanContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "LOAN_CONTRACT_NO", nullable = false, unique = true, length = 20)
    private String loanContractNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BUSINESS_PARTNER_CODE", referencedColumnName = "businessPartnerCode", nullable = false)
    private BusinessPartner businessPartner;

    @Column(name = "LOAN_PRODUCT", nullable = false, length = 100)
    private String loanProduct;

    @Column(name = "PRINCIPAL_AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "DISBURSEMENT_DATE", nullable = false)
    private LocalDate disbursementDate;

    @Column(name = "MATURITY_DATE", nullable = false)
    private LocalDate maturityDate;

    @Column(name = "INTEREST_RATE", nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "REPAYMENT_METHOD", nullable = false, length = 50)
    private String repaymentMethod; // ?먮━湲덇퇏?? 留뚭린?쇱떆 ??

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status; // ACTIVE, PAID_OFF, DEFAULT

    @Column(name = "CURRENT_PRINCIPAL_BALANCE", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentPrincipalBalance;

    @Column(name = "DEFERRED_LOAN_FEE", nullable = false, precision = 19, scale = 2)
    private BigDecimal deferredLoanFee;

    @Column(name = "EFFECTIVE_INTEREST_RATE", nullable = false, precision = 5, scale = 4)
    private BigDecimal effectiveInterestRate;

    // EIR ?곴컖 異붿쟻???좉퇋 ?꾨뱶
    @Column(name = "TOTAL_INTEREST_PAID", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalInterestPaid = BigDecimal.ZERO;

    @Column(name = "TOTAL_PRINCIPAL_PAID", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrincipalPaid = BigDecimal.ZERO;

    // ?곴컖 ?ㅼ?以??뷀듃由ъ???OneToMany 愿怨?
    @OneToMany(mappedBy = "loanContract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LoanAmortizationScheduleEntry> amortizationSchedule = new ArrayList<>();

    @Column(name = "CREATE_DATE", updatable = false, nullable = false)
    private LocalDateTime createDate;

    @Column(name = "UPDATE_DATE", nullable = false)
    private LocalDateTime updateDate;

    @Column(name = "AUDIT_USER", nullable = false, length = 50)
    private String auditUser;

    @PrePersist
    protected void onCreate() {
        createDate = LocalDateTime.now();
        updateDate = LocalDateTime.now();
        if (status == null) status = "ACTIVE";
        if (auditUser == null) auditUser = "SYSTEM";
    }

    @PreUpdate
    protected void onUpdate() {
        updateDate = LocalDateTime.now();
    }

    // [Business Logic] ?곴컖 ?ㅼ?以??앹꽦 (?먮━湲?洹좊벑 ?곹솚 ?덉떆)
    public List<LoanAmortizationScheduleEntry> generateAmortizationSchedule(int totalPeriods) {
        BigDecimal periodicRate = this.interestRate.divide(BigDecimal.valueOf(12), 10, java.math.RoundingMode.HALF_UP);
        
        // ?먮━湲?洹좊벑 ?곹솚??怨꾩궛 怨듭떇: P * r * (1+r)^n / ((1+r)^n - 1)
        BigDecimal onePlusRPowerN = periodicRate.add(BigDecimal.ONE).pow(totalPeriods);
        BigDecimal payment = principalAmount.multiply(periodicRate).multiply(onePlusRPowerN)
                .divide(onePlusRPowerN.subtract(BigDecimal.ONE), 2, java.math.RoundingMode.HALF_UP);

        BigDecimal remainingBalance = principalAmount;
        List<LoanAmortizationScheduleEntry> entries = new java.util.ArrayList<>();

        for (int i = 1; i <= totalPeriods; i++) {
            BigDecimal interest = remainingBalance.multiply(periodicRate).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal principal = payment.subtract(interest);
            
            if (i == totalPeriods) { // 留덉?留??뚯감 ?붿븸 議곗젙
                principal = remainingBalance;
                payment = principal.add(interest);
                remainingBalance = BigDecimal.ZERO;
            } else {
                remainingBalance = remainingBalance.subtract(principal);
            }

            LoanAmortizationScheduleEntry entry = new LoanAmortizationScheduleEntry();
            entry.setLoanContract(this);
            entry.setPeriodNumber(i);
            entry.setPaymentDate(disbursementDate.plusMonths(i));
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

    // ?곌?愿怨??몄쓽 硫붿꽌??
    public void addAmortizationEntry(LoanAmortizationScheduleEntry entry) {
        this.amortizationSchedule.add(entry);
        entry.setLoanContract(this);
    }

    // Getter 諛?Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLoanContractNo() { return loanContractNo; }
    public void setLoanContractNo(String loanContractNo) { this.loanContractNo = loanContractNo; }

    public BusinessPartner getBusinessPartner() { return businessPartner; }
    public void setBusinessPartner(BusinessPartner businessPartner) { this.businessPartner = businessPartner; }

    public String getLoanProduct() { return loanProduct; }
    public void setLoanProduct(String loanProduct) { this.loanProduct = loanProduct; }

    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }

    public LocalDate getDisbursementDate() { return disbursementDate; }
    public void setDisbursementDate(LocalDate disbursementDate) { this.disbursementDate = disbursementDate; }

    public LocalDate getMaturityDate() { return maturityDate; }
    public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }

    public String getRepaymentMethod() { return repaymentMethod; }
    public void setRepaymentMethod(String repaymentMethod) { this.repaymentMethod = repaymentMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getCurrentPrincipalBalance() { return currentPrincipalBalance; }
    public void setCurrentPrincipalBalance(BigDecimal currentPrincipalBalance) { this.currentPrincipalBalance = currentPrincipalBalance; }

    public BigDecimal getDeferredLoanFee() { return deferredLoanFee; }
    public void setDeferredLoanFee(BigDecimal deferredLoanFee) { this.deferredLoanFee = deferredLoanFee; }

    public BigDecimal getEffectiveInterestRate() { return effectiveInterestRate; }
    public void setEffectiveInterestRate(BigDecimal effectiveInterestRate) { this.effectiveInterestRate = effectiveInterestRate; }

    public BigDecimal getTotalInterestPaid() { return totalInterestPaid; }
    public void setTotalInterestPaid(BigDecimal totalInterestPaid) { this.totalInterestPaid = totalInterestPaid; }

    public BigDecimal getTotalPrincipalPaid() { return totalPrincipalPaid; }
    public void setTotalPrincipalPaid(BigDecimal totalPrincipalPaid) { this.totalPrincipalPaid = totalPrincipalPaid; }

    public List<LoanAmortizationScheduleEntry> getAmortizationSchedule() { return amortizationSchedule; }
    public void setAmortizationSchedule(List<LoanAmortizationScheduleEntry> amortizationSchedule) { this.amortizationSchedule = amortizationSchedule; }

    public LocalDateTime getCreateDate() { return createDate; }
    public void setCreateDate(LocalDateTime createDate) { this.createDate = createDate; }

    public LocalDateTime getUpdateDate() { return updateDate; }
    public void setUpdateDate(LocalDateTime updateDate) { this.updateDate = updateDate; }

    public String getAuditUser() { return auditUser; }
    public void setAuditUser(String auditUser) { this.auditUser = auditUser; }
}
