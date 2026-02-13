package com.ho.account.loan.domain;

import com.ho.account.journal.domain.JournalEntry;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 재계산 실행 (Recalculation Run) 엔티티
 * 중도상환, 조건 변경 등으로 인해 유효이자율(EIR) 스케줄이 재계산된 이력을 관리합니다.
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
    private String impactAnalysis; // 재계산 영향 분석 (JSON 또는 텍스트)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adjustment_journal_entry_id")
    private JournalEntry adjustmentJournalEntry; // 재계산으로 인한 조정 분개 전표

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

    // Getters and Setters
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

    public JournalEntry getAdjustmentJournalEntry() {
        return adjustmentJournalEntry;
    }

    public void setAdjustmentJournalEntry(JournalEntry adjustmentJournalEntry) {
        this.adjustmentJournalEntry = adjustmentJournalEntry;
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
