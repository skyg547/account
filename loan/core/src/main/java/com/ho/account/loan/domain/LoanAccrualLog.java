package com.ho.account.loan.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "LOAN_ACCRUAL_LOG",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_loan_accrual_log_loan_date",
                columnNames = {"LOAN_ID", "ACCRUAL_DATE"}))
public class LoanAccrualLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ACCRUAL_DATE", nullable = false)
    private LocalDate accrualDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LOAN_ID", nullable = false)
    private Loan loan;

    @Column(name = "ACCRUED_AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal accruedAmount;

    @Column(name = "JOURNAL_ENTRY_ID")
    private Long journalEntryId; // journal-ledger 전표 ID 값 참조

    @Column(name = "JOURNAL_NO", length = 20)
    private String journalNo; // journal-ledger 전표번호 값 참조

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private AccrualStatus status;

    @Column(name = "ERROR_MESSAGE", length = 1000)
    private String errorMessage;

    @Column(name = "CREATE_DATE", updatable = false, nullable = false)
    private LocalDateTime createDate;

    @Column(name = "UPDATE_DATE", nullable = false)
    private LocalDateTime updateDate;

    @Column(name = "AUDIT_USER", nullable = false, length = 50)
    private String auditUser;

    public enum AccrualStatus {
        PENDING, SUCCESS, FAILED
    }

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

    // Getter 및 Setter
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

    public Loan getLoan() {
        return loan;
    }

    public void setLoan(Loan loan) {
        this.loan = loan;
    }

    public BigDecimal getAccruedAmount() {
        return accruedAmount;
    }

    public void setAccruedAmount(BigDecimal accruedAmount) {
        this.accruedAmount = accruedAmount;
    }

    public Long getJournalEntryId() {
        return journalEntryId;
    }

    public void setJournalEntryId(Long journalEntryId) {
        this.journalEntryId = journalEntryId;
    }

    public String getJournalNo() {
        return journalNo;
    }

    public void setJournalNo(String journalNo) {
        this.journalNo = journalNo;
    }

    public AccrualStatus getStatus() {
        return status;
    }

    public void setStatus(AccrualStatus status) {
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

    public static LoanAccrualLog start(
            Loan loan,
            LocalDate accrualDate,
            BigDecimal accruedAmount,
            String actor) {
        if (loan == null || loan.getId() == null) {
            throw new IllegalArgumentException("A persisted loan is required.");
        }
        if (accrualDate == null) {
            throw new IllegalArgumentException("accrualDate is required.");
        }
        if (accruedAmount == null || accruedAmount.signum() <= 0) {
            throw new IllegalArgumentException("accruedAmount must be positive.");
        }
        LoanAccrualLog log = new LoanAccrualLog();
        log.loan = loan;
        log.accrualDate = accrualDate;
        log.accruedAmount = accruedAmount;
        log.status = AccrualStatus.PENDING;
        log.auditUser = requireActor(actor);
        return log;
    }

    public void prepareRetry(BigDecimal amount, String actor) {
        if (status == AccrualStatus.SUCCESS) {
            throw new IllegalStateException("A successful accrual cannot be retried.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("accruedAmount must be positive.");
        }
        accruedAmount = amount;
        journalEntryId = null;
        journalNo = null;
        errorMessage = null;
        status = AccrualStatus.PENDING;
        auditUser = requireActor(actor);
    }

    public void markSuccess(Long postedJournalEntryId, String postedSlipNo) {
        if (postedJournalEntryId == null || postedJournalEntryId < 1) {
            throw new IllegalArgumentException("journalEntryId must be positive.");
        }
        if (postedSlipNo == null || postedSlipNo.isBlank()) {
            throw new IllegalArgumentException("journalNo is required.");
        }
        journalEntryId = postedJournalEntryId;
        journalNo = postedSlipNo.trim();
        errorMessage = null;
        status = AccrualStatus.SUCCESS;
    }

    public void markFailed(RuntimeException failure) {
        if (failure == null) {
            throw new IllegalArgumentException("failure is required.");
        }
        String message = failure.getMessage();
        errorMessage = message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : message.substring(0, Math.min(message.length(), 1000));
        status = AccrualStatus.FAILED;
    }

    private static String requireActor(String actor) {
        if (actor == null || actor.isBlank() || actor.trim().length() > 50) {
            throw new IllegalArgumentException("actor is required and must not exceed 50 characters.");
        }
        return actor.trim();
    }
}
