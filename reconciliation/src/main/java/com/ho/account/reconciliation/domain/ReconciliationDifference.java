package com.ho.account.reconciliation.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ???李⑥씠(Reconciliation Difference) ?뷀떚??
 * ????ㅽ뻾 寃곌낵 諛쒓껄??李⑥씠 ??ぉ??湲곕줉?섍퀬, ?ъ쑀 肄붾뱶 諛?議곗젙 ?꾪몴? ?곌퀎?⑸땲??
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
    private DifferenceType differenceType; // 李⑥씠 ?좏삎 (MISSING_SOURCE, MISSING_TARGET, AMOUNT_MISMATCH ??

    @Column(precision = 19, scale = 2)
    private BigDecimal amountExpected; // 湲곕? 湲덉븸

    @Column(precision = 19, scale = 2)
    private BigDecimal amountActual; // ?ㅼ젣 湲덉븸

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal differenceAmount; // 李⑥씠 湲덉븸

    @Column(length = 1000)
    private String description; // 李⑥씠 ?곸꽭 ?ㅻ챸

    // ?먯쿇/????곗씠????ぉ?????李몄“ (?? JSON 臾몄옄?대줈 { "type": "BANK_TRANSACTION", "id": "TXN123" } ?먮뒗 { "type": "JOURNAL_ENTRY_DETAIL", "id": "JD456" })
    @Column(columnDefinition = "TEXT")
    private String sourceItemRef;

    @Column(columnDefinition = "TEXT")
    private String targetItemRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reason_code_id")
    private DifferenceReasonCode reasonCode; // 李⑥씠 ?ъ쑀 肄붾뱶 (DoD: 李⑥씠??"?먯씤肄붾뱶"濡?諛섎뱶???섎졃)

    @Column(name = "adjustment_journal_entry_id")
    private Long adjustmentJournalEntryId; // 議곗젙 ?꾪몴 ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReconciliationDifferenceStatus status; // 李⑥씠 泥섎━ ?곹깭 (PENDING, ASSIGNED, RESOLVED, IGNORED)

    @Column(length = 50)
    private String assignedToUser; // ?대떦??(User ?뷀떚?곗? FK ?곌껐 媛?ν븯?? ?쇰떒 String?쇰줈)

    private LocalDateTime slaDueDate; // SLA 湲고븳

    private LocalDateTime resolvedAt; // ?닿껐 ?쇱떆

    @Column(length = 50)
    private String resolvedBy; // ?닿껐??

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
        AMOUNT_MISMATCH, // 湲덉븸 遺덉씪移?
        MISSING_SOURCE,  // ?먯쿇 ?곗씠???꾨씫
        MISSING_TARGET,  // ????곗씠???꾨씫
        DATE_MISMATCH,   // ?좎쭨 遺덉씪移?
        OTHER            // 湲고?
    }

    public enum ReconciliationDifferenceStatus {
        PENDING,   // 泥섎━ ?湲?
        ASSIGNED,  // ?대떦???좊떦
        IN_REVIEW, // 寃??以?
        RESOLVED,  // ?닿껐 ?꾨즺
        IGNORED    // 臾댁떆??
    }

    // --- Getter 諛?Setter ---

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

    public Long getAdjustmentJournalEntryId() {
        return adjustmentJournalEntryId;
    }

    public void setAdjustmentJournalEntryId(Long adjustmentJournalEntryId) {
        this.adjustmentJournalEntryId = adjustmentJournalEntryId;
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
