package com.ho.account.reconciliation.domain;

import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ?Ä??Ï∞®Ïù¥(Reconciliation Difference) ?îÌã∞??
 * ?Ä???§Ìñâ Í≤∞Í≥º Î∞úÍ≤¨??Ï∞®Ïù¥ ??™©??Í∏∞Î°ù?òÍ≥†, ?¨Ïú† ÏΩîÎìú Î∞?Ï°∞Ï†ï ?ÑÌëú?Ä ?∞Í≥Ñ?©Îãà??
 */
@Entity
@Table(name = "reconciliation_differences")
public class ReconciliationDifference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_run_id", nullable = false)
    private ReconciliationRun reconciliationRun;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DifferenceType differenceType; // Ï∞®Ïù¥ ?†Ìòï (MISSING_SOURCE, MISSING_TARGET, AMOUNT_MISMATCH ??

    @Column(precision = 19, scale = 2)
    private BigDecimal amountExpected; // Í∏∞Î? Í∏àÏï°

    @Column(precision = 19, scale = 2)
    private BigDecimal amountActual; // ?§Ï†ú Í∏àÏï°

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal differenceAmount; // Ï∞®Ïù¥ Í∏àÏï°

    @Column(length = 1000)
    private String description; // Ï∞®Ïù¥ ?ÅÏÑ∏ ?§Î™Ö

    // ?êÏ≤ú/?Ä???∞Ïù¥????™©???Ä??Ï∞∏Ï°∞ (?? JSON Î¨∏Ïûê?¥Î°ú { "type": "BANK_TRANSACTION", "id": "TXN123" } ?êÎäî { "type": "JOURNAL_ENTRY_DETAIL", "id": "JD456" })
    @Column(columnDefinition = "TEXT")
    private String sourceItemRef;

    @Column(columnDefinition = "TEXT")
    private String targetItemRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reason_code_id")
    private DifferenceReasonCode reasonCode; // Ï∞®Ïù¥ ?¨Ïú† ÏΩîÎìú (DoD: Ï∞®Ïù¥??"?êÏù∏ÏΩîÎìú"Î°?Î∞òÎìú???òÎ†¥)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adjustment_journal_entry_id")
    private JournalEntry adjustmentJournalEntry; // Ï°∞Ï†ï ?ÑÌëú (DoD: "Ï°∞Ï†ï?ÑÌëú ÎßÅÌÅ¨"Î°?Î∞òÎìú???òÎ†¥)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReconciliationDifferenceStatus status; // Ï∞®Ïù¥ Ï≤òÎ¶¨ ?ÅÌÉú (PENDING, ASSIGNED, RESOLVED, IGNORED)

    @Column(length = 50)
    private String assignedToUser; // ?¥Îãπ??(User ?îÌã∞?∞Ï? FK ?∞Í≤∞ Í∞Ä?•Ìïò?? ?ºÎã® String?ºÎ°ú)

    private LocalDateTime slaDueDate; // SLA Í∏∞Ìïú

    private LocalDateTime resolvedAt; // ?¥Í≤∞ ?ºÏãú

    @Column(length = 50)
    private String resolvedBy; // ?¥Í≤∞??

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.auditUser == null) {
            this.auditUser = "SYSTEM";
        }
        if (this.status == null) {
            this.status = ReconciliationDifferenceStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Enums ---
    public enum DifferenceType {
        AMOUNT_MISMATCH, // Í∏àÏï° Î∂àÏùºÏπ?
        MISSING_SOURCE,  // ?êÏ≤ú ?∞Ïù¥???ÑÎùΩ
        MISSING_TARGET,  // ?Ä???∞Ïù¥???ÑÎùΩ
        DATE_MISMATCH,   // ?†Ïßú Î∂àÏùºÏπ?
        OTHER            // Í∏∞Ì?
    }

    public enum ReconciliationDifferenceStatus {
        PENDING,   // Ï≤òÎ¶¨ ?ÄÍ∏?
        ASSIGNED,  // ?¥Îãπ???†Îãπ
        IN_REVIEW, // Í≤Ä??Ï§?
        RESOLVED,  // ?¥Í≤∞ ?ÑÎ£å
        IGNORED    // Î¨¥Ïãú??
    }

    // --- Getter Î∞?Setter ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ReconciliationRun getReconciliationRun() {
        return reconciliationRun;
    }

    public void setReconciliationRun(ReconciliationRun reconciliationRun) {
        this.reconciliationRun = reconciliationRun;
    }

    public DifferenceType getDifferenceType() {
        return differenceType;
    }

    public void setDifferenceType(DifferenceType differenceType) {
        this.differenceType = differenceType;
    }

    public BigDecimal getAmountExpected() {
        return amountExpected;
    }

    public void setAmountExpected(BigDecimal amountExpected) {
        this.amountExpected = amountExpected;
    }

    public BigDecimal getAmountActual() {
        return amountActual;
    }

    public void setAmountActual(BigDecimal amountActual) {
        this.amountActual = amountActual;
    }

    public BigDecimal getDifferenceAmount() {
        return differenceAmount;
    }

    public void setDifferenceAmount(BigDecimal differenceAmount) {
        this.differenceAmount = differenceAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceItemRef() {
        return sourceItemRef;
    }

    public void setSourceItemRef(String sourceItemRef) {
        this.sourceItemRef = sourceItemRef;
    }

    public String getTargetItemRef() {
        return targetItemRef;
    }

    public void setTargetItemRef(String targetItemRef) {
        this.targetItemRef = targetItemRef;
    }

    public DifferenceReasonCode getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(DifferenceReasonCode reasonCode) {
        this.reasonCode = reasonCode;
    }

    public JournalEntry getAdjustmentJournalEntry() {
        return adjustmentJournalEntry;
    }

    public void setAdjustmentJournalEntry(JournalEntry adjustmentJournalEntry) {
        this.adjustmentJournalEntry = adjustmentJournalEntry;
    }

    public ReconciliationDifferenceStatus getStatus() {
        return status;
    }

    public void setStatus(ReconciliationDifferenceStatus status) {
        this.status = status;
    }

    public String getAssignedToUser() {
        return assignedToUser;
    }

    public void setAssignedToUser(String assignedToUser) {
        this.assignedToUser = assignedToUser;
    }

    public LocalDateTime getSlaDueDate() {
        return slaDueDate;
    }

    public void setSlaDueDate(LocalDateTime slaDueDate) {
        this.slaDueDate = slaDueDate;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
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
