package com.ho.account.loan.domain;

import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ?¨Í≥Ñ???§Ìñâ (Recalculation Run) ?îÌã∞??
 * Ï§ëÎèÑ?ÅÌôò, Ï°∞Í±¥ Î≥ÄÍ≤??±ÏúºÎ°??∏Ìï¥ ?†Ìö®?¥Ïûê??EIR) ?§Ï?Ï§ÑÏù¥ ?¨Í≥Ñ?∞Îêú ?¥Î†•??Í¥ÄÎ¶¨Ìï©?àÎã§.
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
    private LocalDate recalculationDate; // ?¨Í≥Ñ???§Ìñâ??

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RecalculationReason reason; // ?¨Í≥Ñ???¨Ïú† (EARLY_REPAYMENT, CONDITION_CHANGE ??

    @Column(precision = 5, scale = 4)
    private BigDecimal oldEIR; // ?¥Ï†Ñ ?†Ìö®?¥Ïûê??

    @Column(precision = 5, scale = 4)
    private BigDecimal newEIR; // ?àÎ°ú???†Ìö®?¥Ïûê??

    private LocalDate oldMaturityDate; // ?¥Ï†Ñ ÎßåÍ∏∞??

    private LocalDate newMaturityDate; // ?àÎ°ú??ÎßåÍ∏∞??

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recalculated_amortization_schedule_start_id")
    private EIRAmortizationSchedule recalculatedAmortizationScheduleStart; // ?¨Í≥Ñ?∞Îêú ?§Ï?Ï§ÑÏùò ?úÏûë ??™©

    @Column(columnDefinition = "TEXT")
    private String impactAnalysis; // ?¨Í≥Ñ???ÅÌñ• Î∂ÑÏÑù (JSON ?êÎäî ?çÏä§??

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adjustment_journal_entry_id")
    private JournalEntry adjustmentJournalEntry; // ?¨Í≥Ñ?∞ÏúºÎ°??∏Ìïú Ï°∞Ï†ï Î∂ÑÍ∞ú ?ÑÌëú

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

    // Getter Î∞?Setter
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
