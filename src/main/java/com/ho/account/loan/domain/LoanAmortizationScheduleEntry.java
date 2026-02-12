package com.ho.account.loan.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "LOAN_AMORTIZATION_SCHEDULE_ENTRIES")
public class LoanAmortizationScheduleEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LOAN_CONTRACT_ID", nullable = false)
    private LoanContract loanContract;

    @Column(name = "PAYMENT_DATE", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "PERIOD_NUMBER", nullable = false)
    private Integer periodNumber;

    @Column(name = "STARTING_BALANCE", nullable = false, precision = 19, scale = 2)
    private BigDecimal startingBalance;

    @Column(name = "SCHEDULED_PAYMENT_AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal scheduledPaymentAmount;

    @Column(name = "INTEREST_AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal interestAmount;

    @Column(name = "PRINCIPAL_AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "ENDING_BALANCE", nullable = false, precision = 19, scale = 2)
    private BigDecimal endingBalance;

    @Column(name = "DEFERRED_FEE_AMORTIZATION", precision = 19, scale = 2)
    private BigDecimal deferredFeeAmortization; // EIR 상각액 (선택적)

    @Column(name = "ENTRY_TYPE", length = 50)
    private String entryType; // REPAYMENT, FEE, DISBURSEMENT, etc.

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
        if (auditUser == null) auditUser = "SYSTEM";
    }

    @PreUpdate
    protected void onUpdate() {
        updateDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LoanContract getLoanContract() { return loanContract; }
    public void setLoanContract(LoanContract loanContract) { this.loanContract = loanContract; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public Integer getPeriodNumber() { return periodNumber; }
    public void setPeriodNumber(Integer periodNumber) { this.periodNumber = periodNumber; }

    public BigDecimal getStartingBalance() { return startingBalance; }
    public void setStartingBalance(BigDecimal startingBalance) { this.startingBalance = startingBalance; }

    public BigDecimal getScheduledPaymentAmount() { return scheduledPaymentAmount; }
    public void setScheduledPaymentAmount(BigDecimal scheduledPaymentAmount) { this.scheduledPaymentAmount = scheduledPaymentAmount; }

    public BigDecimal getInterestAmount() { return interestAmount; }
    public void setInterestAmount(BigDecimal interestAmount) { this.interestAmount = interestAmount; }

    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }

    public BigDecimal getEndingBalance() { return endingBalance; }
    public void setEndingBalance(BigDecimal endingBalance) { this.endingBalance = endingBalance; }

    public BigDecimal getDeferredFeeAmortization() { return deferredFeeAmortization; }
    public void setDeferredFeeAmortization(BigDecimal deferredFeeAmortization) { this.deferredFeeAmortization = deferredFeeAmortization; }

    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }

    public LocalDateTime getCreateDate() { return createDate; }
    public void setCreateDate(LocalDateTime createDate) { this.createDate = createDate; }

    public LocalDateTime getUpdateDate() { return updateDate; }
    public void setUpdateDate(LocalDateTime updateDate) { this.updateDate = updateDate; }

    public String getAuditUser() { return auditUser; }
    public void setAuditUser(String auditUser) { this.auditUser = auditUser; }
}
