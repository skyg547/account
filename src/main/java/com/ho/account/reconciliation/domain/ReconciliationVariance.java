package com.ho.account.reconciliation.domain;

import com.ho.account.journal.domain.JournalEntry;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "RECONCILIATION_VARIANCES")
public class ReconciliationVariance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RECONCILIATION_RESULT_ID", nullable = false)
    private ReconciliationResult reconciliationResult;

    @Column(name = "VARIANCE_CODE", nullable = false, length = 50)
    private String varianceCode;

    @Column(name = "DESCRIPTION", nullable = false, length = 500)
    private String description;

    @Column(name = "AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "DR_CR_TYPE", length = 10)
    private String drCrType;

    @Column(name = "SOURCE_REFERENCE", length = 255)
    private String sourceReference;

    @Column(name = "TARGET_REFERENCE", length = 255)
    private String targetReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ADJUSTMENT_JOURNAL_ENTRY_ID")
    private JournalEntry adjustmentJournalEntry;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 50)
    private VarianceStatus status;

    @Column(name = "RESOLVED_BY", length = 50)
    private String resolvedBy;

    @Column(name = "RESOLVED_AT")
    private LocalDateTime resolvedAt;

    @Column(name = "CREATE_DATE", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Column(name = "UPDATE_DATE", nullable = false)
    private LocalDateTime updateDate;

    @Column(name = "AUDIT_USER", nullable = false, length = 50)
    private String auditUser;

    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
        this.updateDate = LocalDateTime.now();
        if (this.auditUser == null) {
            this.auditUser = "SYSTEM";
        }
        if (this.status == null) {
            this.status = VarianceStatus.OPEN;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ReconciliationResult getReconciliationResult() {
        return reconciliationResult;
    }

    public void setReconciliationResult(ReconciliationResult reconciliationResult) {
        this.reconciliationResult = reconciliationResult;
    }

    public String getVarianceCode() {
        return varianceCode;
    }

    public void setVarianceCode(String varianceCode) {
        this.varianceCode = varianceCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDrCrType() {
        return drCrType;
    }

    public void setDrCrType(String drCrType) {
        this.drCrType = drCrType;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public void setSourceReference(String sourceReference) {
        this.sourceReference = sourceReference;
    }

    public String getTargetReference() {
        return targetReference;
    }

    public void setTargetReference(String targetReference) {
        this.targetReference = targetReference;
    }

    public JournalEntry getAdjustmentJournalEntry() {
        return adjustmentJournalEntry;
    }

    public void setAdjustmentJournalEntry(JournalEntry adjustmentJournalEntry) {
        this.adjustmentJournalEntry = adjustmentJournalEntry;
    }

    public VarianceStatus getStatus() {
        return status;
    }

    public void setStatus(VarianceStatus status) {
        this.status = status;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
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
