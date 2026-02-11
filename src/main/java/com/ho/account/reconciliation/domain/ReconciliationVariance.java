package com.ho.account.reconciliation.domain;

import com.ho.account.journal.domain.JournalEntry;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reconciliation_variances")
public class ReconciliationVariance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_result_id", nullable = false)
    private ReconciliationResult reconciliationResult;

    @Column(nullable = false, length = 50)
    private String varianceCode;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 10)
    private String drCrType; // DEBIT or CREDIT for the variance

    private String sourceReference; // e.g., original document number, transaction ID
    private String targetReference; // e.g., journal entry ID, ledger line ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adjustment_journal_entry_id")
    private JournalEntry adjustmentJournalEntry; // Link to the adjustment journal entry

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VarianceStatus status;

    private String resolvedBy;
    private LocalDateTime resolvedAt;

    public ReconciliationVariance() {
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ReconciliationResult getReconciliationResult() { return reconciliationResult; }
    public void setReconciliationResult(ReconciliationResult reconciliationResult) { this.reconciliationResult = reconciliationResult; }

    public String getVarianceCode() { return varianceCode; }
    public void setVarianceCode(String varianceCode) { this.varianceCode = varianceCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDrCrType() { return drCrType; }
    public void setDrCrType(String drCrType) { this.drCrType = drCrType; }

    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String sourceReference) { this.sourceReference = sourceReference; }

    public String getTargetReference() { return targetReference; }
    public void setTargetReference(String targetReference) { this.targetReference = targetReference; }

    public JournalEntry getAdjustmentJournalEntry() { return adjustmentJournalEntry; }
    public void setAdjustmentJournalEntry(JournalEntry adjustmentJournalEntry) { this.adjustmentJournalEntry = adjustmentJournalEntry; }

    public VarianceStatus getStatus() { return status; }
    public void setStatus(VarianceStatus status) { this.status = status; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
