package com.ho.account.loan.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 대출 실행(Loan Disbursal) 엔티티.
 *
 * <p>대출금이 실제로 지급된 이력을 기록하고, 관련 전표 ID와 전표번호를 값으로 연결합니다.
 */
@Entity
@Table(
        name = "loan_disbursals",
        uniqueConstraints = @UniqueConstraint(name = "uq_loan_disbursal_loan", columnNames = "loan_id"))
public class LoanDisbursal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(nullable = false)
    private LocalDate disbursalDate; // 실제 실행일

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal disbursedAmount; // 실행 금액

    @Column(name = "journal_entry_id")
    private Long journalEntryId; // 타 bounded context 전표는 ID로 참조합니다.

    @Column(name = "journal_entry_slip_no", length = 30)
    private String journalEntrySlipNo;

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
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getter and Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Loan getLoan() {
        return loan;
    }

    public void setLoan(Loan loan) {
        this.loan = loan;
    }

    public LocalDate getDisbursalDate() {
        return disbursalDate;
    }

    public void setDisbursalDate(LocalDate disbursalDate) {
        this.disbursalDate = disbursalDate;
    }

    public BigDecimal getDisbursedAmount() {
        return disbursedAmount;
    }

    public void setDisbursedAmount(BigDecimal disbursedAmount) {
        this.disbursedAmount = disbursedAmount;
    }

    public Long getJournalEntryId() {
        return journalEntryId;
    }

    public void setJournalEntryId(Long journalEntryId) {
        this.journalEntryId = journalEntryId;
    }

    public String getJournalEntrySlipNo() {
        return journalEntrySlipNo;
    }

    public void setJournalEntrySlipNo(String journalEntrySlipNo) {
        this.journalEntrySlipNo = journalEntrySlipNo;
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

    public static LoanDisbursal recordDisbursal(
            Loan loan,
            LocalDate disbursalDate,
            BigDecimal disbursedAmount,
            String actor) {
        if (loan == null) {
            throw new IllegalArgumentException("loan is required.");
        }
        if (disbursalDate == null) {
            throw new IllegalArgumentException("disbursalDate is required.");
        }
        if (disbursedAmount == null || disbursedAmount.signum() <= 0) {
            throw new IllegalArgumentException("disbursedAmount must be positive.");
        }
        LoanDisbursal disbursal = new LoanDisbursal();
        disbursal.loan = loan;
        disbursal.disbursalDate = disbursalDate;
        disbursal.disbursedAmount = disbursedAmount;
        disbursal.auditUser = requireActor(actor);
        return disbursal;
    }

    public void linkPostedJournal(Long journalEntryId, String slipNo) {
        if (journalEntryId == null || journalEntryId < 1) {
            throw new IllegalArgumentException("journalEntryId must be positive.");
        }
        if (slipNo == null || slipNo.isBlank()) {
            throw new IllegalArgumentException("journal slipNo is required.");
        }
        this.journalEntryId = journalEntryId;
        this.journalEntrySlipNo = slipNo.trim();
    }

    private static String requireActor(String actor) {
        if (actor == null || actor.isBlank() || actor.trim().length() > 50) {
            throw new IllegalArgumentException("actor is required and must not exceed 50 characters.");
        }
        return actor.trim();
    }
}
