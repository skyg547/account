package com.ho.account.loan.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "LOAN_ACCRUAL_LOG")
public class LoanAccrualLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ACCRUAL_DATE", nullable = false)
    private LocalDate accrualDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LOAN_CONTRACT_ID", nullable = false)
    private LoanContract loanContract;

    @Column(name = "ACCRUED_AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal accruedAmount;

    @Column(name = "JOURNAL_NO", length = 20)
    private String journalNo;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status; // SUCCESS, FAILED

    @Column(name = "ERROR_MESSAGE", length = 1000)
    private String errorMessage;

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
        if (auditUser == null)
            auditUser = "SYSTEM";
    }

    @PreUpdate
    protected void onUpdate() {
        updateDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getAccrualDate() {
        return accrualDate;
    }

    public void setAccrualDate(LocalDate accrualDate) {
        this.accrualDate = accrualDate;
    }

    public LoanContract getLoanContract() {
        return loanContract;
    }

    public void setLoanContract(LoanContract loanContract) {
        this.loanContract = loanContract;
    }

    public BigDecimal getAccruedAmount() {
        return accruedAmount;
    }

    public void setAccruedAmount(BigDecimal accruedAmount) {
        this.accruedAmount = accruedAmount;
    }

    public String getJournalNo() {
        return journalNo;
    }

    public void setJournalNo(String journalNo) {
        this.journalNo = journalNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }

    public String getAuditUser() {
        return auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }
}
