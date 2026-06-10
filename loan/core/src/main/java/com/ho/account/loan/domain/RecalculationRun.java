package com.ho.account.loan.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ?ш퀎???ㅽ뻾 (Recalculation Run) ?뷀떚??
 * 以묐룄?곹솚, 議곌굔 蹂寃??깆쑝濡??명빐 ?좏슚?댁옄??EIR) ?ㅼ?以꾩씠 ?ш퀎?곕맂 ?대젰??愿由ы빀?덈떎.
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
    private LocalDate recalculationDate; // ?ш퀎???ㅽ뻾??

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RecalculationReason reason; // ?ш퀎???ъ쑀 (EARLY_REPAYMENT, CONDITION_CHANGE ??

    @Column(precision = 5, scale = 4)
    private BigDecimal oldEIR; // ?댁쟾 ?좏슚?댁옄??

    @Column(precision = 5, scale = 4)
    private BigDecimal newEIR; // ?덈줈???좏슚?댁옄??

    private LocalDate oldMaturityDate; // ?댁쟾 留뚭린??

    private LocalDate newMaturityDate; // ?덈줈??留뚭린??

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recalculated_amortization_schedule_start_id")
    private EIRAmortizationSchedule recalculatedAmortizationScheduleStart; // ?ш퀎?곕맂 ?ㅼ?以꾩쓽 ?쒖옉 ??ぉ

    @Column(columnDefinition = "TEXT")
    private String impactAnalysis; // ?ш퀎???곹뼢 遺꾩꽍 (JSON ?먮뒗 ?띿뒪??

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

    // Getter 諛?Setter
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
}
