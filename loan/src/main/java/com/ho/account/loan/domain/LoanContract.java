package com.ho.account.loan.domain;

import com.ho.account.basic.domain.BusinessPartner;
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
    private String repaymentMethod; // 원리금균등, 만기일시 등

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status; // ACTIVE, PAID_OFF, DEFAULT

    @Column(name = "CURRENT_PRINCIPAL_BALANCE", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentPrincipalBalance;

    @Column(name = "DEFERRED_LOAN_FEE", nullable = false, precision = 19, scale = 2)
    private BigDecimal deferredLoanFee;

    @Column(name = "EFFECTIVE_INTEREST_RATE", nullable = false, precision = 5, scale = 4)
    private BigDecimal effectiveInterestRate;

    // EIR 상각 추적용 신규 필드
    @Column(name = "TOTAL_INTEREST_PAID", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalInterestPaid = BigDecimal.ZERO;

    @Column(name = "TOTAL_PRINCIPAL_PAID", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrincipalPaid = BigDecimal.ZERO;

    // 상각 스케줄 엔트리와의 OneToMany 관계
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

    // 연관관계 편의 메서드 (필요 시 스케줄에 엔트리를 추가하기 위한)
    public void addAmortizationEntry(LoanAmortizationScheduleEntry entry) {
        amortizationSchedule.add(entry);
        entry.setLoanContract(this);
    }

    // Getter 및 Setter
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
