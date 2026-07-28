package com.ho.account.loan.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 재계산 실행(Recalculation Run) 엔티티.
 *
 * <p>중도상환, 조건 변경, 리스케줄 등으로 EIR과 상각 스케줄을 다시 계산한 이력을 관리합니다.
 */
@Entity
@Table(name = "recalculation_runs")
public class RecalculationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(nullable = false)
    private LocalDate recalculationDate; // 재계산 실행일

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RecalculationReason reason; // 재계산 사유 (EARLY_REPAYMENT, CONDITION_CHANGE 등)

    @Column(precision = 5, scale = 4)
    private BigDecimal oldEIR; // 이전 유효이자율

    @Column(precision = 5, scale = 4)
    private BigDecimal newEIR; // 새로운 유효이자율

    private LocalDate oldMaturityDate; // 이전 만기일

    private LocalDate newMaturityDate; // 새로운 만기일

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recalculated_amortization_schedule_start_id")
    private EIRAmortizationSchedule recalculatedAmortizationScheduleStart; // 재계산된 스케줄의 시작 항목

    @Column(columnDefinition = "TEXT")
    private String impactAnalysis; // 재계산 영향 분석(JSON 또는 텍스트)

    @Column(name = "adjustment_journal_entry_id")
    private Long adjustmentJournalEntryId;

    @Column(name = "adjustment_journal_entry_slip_no", length = 30)
    private String adjustmentJournalEntrySlipNo;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    public enum RecalculationReason {
        EARLY_REPAYMENT, CONDITION_CHANGE, RESCHEDULE, OTHER
    }

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

    public LocalDate getRecalculationDate() {
        return recalculationDate;
    }

    public void setRecalculationDate(LocalDate recalculationDate) {
        this.recalculationDate = recalculationDate;
    }

    public RecalculationReason getReason() {
        return reason;
    }

    public void setReason(RecalculationReason reason) {
        this.reason = reason;
    }

    public BigDecimal getOldEIR() {
        return oldEIR;
    }

    public void setOldEIR(BigDecimal oldEIR) {
        this.oldEIR = oldEIR;
    }

    public BigDecimal getNewEIR() {
        return newEIR;
    }

    public void setNewEIR(BigDecimal newEIR) {
        this.newEIR = newEIR;
    }

    public LocalDate getOldMaturityDate() {
        return oldMaturityDate;
    }

    public void setOldMaturityDate(LocalDate oldMaturityDate) {
        this.oldMaturityDate = oldMaturityDate;
    }

    public LocalDate getNewMaturityDate() {
        return newMaturityDate;
    }

    public void setNewMaturityDate(LocalDate newMaturityDate) {
        this.newMaturityDate = newMaturityDate;
    }

    public EIRAmortizationSchedule getRecalculatedAmortizationScheduleStart() {
        return recalculatedAmortizationScheduleStart;
    }

    public void setRecalculatedAmortizationScheduleStart(EIRAmortizationSchedule recalculatedAmortizationScheduleStart) {
        this.recalculatedAmortizationScheduleStart = recalculatedAmortizationScheduleStart;
    }

    public String getImpactAnalysis() {
        return impactAnalysis;
    }

    public void setImpactAnalysis(String impactAnalysis) {
        this.impactAnalysis = impactAnalysis;
    }

    public Long getAdjustmentJournalEntryId() {
        return adjustmentJournalEntryId;
    }

    public void setAdjustmentJournalEntryId(Long adjustmentJournalEntryId) {
        this.adjustmentJournalEntryId = adjustmentJournalEntryId;
    }

    public String getAdjustmentJournalEntrySlipNo() {
        return adjustmentJournalEntrySlipNo;
    }

    public void setAdjustmentJournalEntrySlipNo(String adjustmentJournalEntrySlipNo) {
        this.adjustmentJournalEntrySlipNo = adjustmentJournalEntrySlipNo;
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

    public static RecalculationRun record(
            Loan loan,
            LocalDate recalculationDate,
            RecalculationReason reason,
            BigDecimal oldEir,
            BigDecimal newEir,
            LocalDate oldMaturityDate,
            LocalDate newMaturityDate,
            EIRAmortizationSchedule firstRecalculatedSchedule,
            String impactAnalysis,
            String actor) {
        if (loan == null || loan.getId() == null) {
            throw new IllegalArgumentException("A persisted loan is required.");
        }
        RecalculationRun run = new RecalculationRun();
        run.loan = loan;
        run.recalculationDate = java.util.Objects.requireNonNull(
                recalculationDate, "recalculationDate is required.");
        run.reason = java.util.Objects.requireNonNull(reason, "reason is required.");
        run.oldEIR = oldEir;
        run.newEIR = java.util.Objects.requireNonNull(newEir, "newEIR is required.");
        run.oldMaturityDate = oldMaturityDate;
        run.newMaturityDate = java.util.Objects.requireNonNull(newMaturityDate, "newMaturityDate is required.");
        run.recalculatedAmortizationScheduleStart = firstRecalculatedSchedule;
        run.impactAnalysis = requireText(impactAnalysis, "impactAnalysis", 4000);
        run.auditUser = requireText(actor, "actor", 50);
        return run;
    }

    public void linkAdjustmentJournal(Long journalEntryId, String slipNo) {
        if (journalEntryId == null || journalEntryId < 1) {
            throw new IllegalArgumentException("journalEntryId must be positive.");
        }
        adjustmentJournalEntryId = journalEntryId;
        adjustmentJournalEntrySlipNo = requireText(slipNo, "slipNo", 30);
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
