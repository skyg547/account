package com.ho.account.reconciliation.dto;

import com.ho.account.reconciliation.domain.ReconciliationVariance;
import com.ho.account.reconciliation.domain.VarianceStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VarianceDto {

    private Long id;
    private Long reconciliationResultId;
    private String varianceCode;
    private String description;
    private BigDecimal amount;
    private String drCrType;
    private String sourceReference;
    private String targetReference;
    private Long adjustmentJournalEntryId;
    private VarianceStatus status;
    private String resolvedBy;
    private LocalDateTime resolvedAt;

    public static VarianceDto from(ReconciliationVariance entity) {
        VarianceDto dto = new VarianceDto();
        dto.setId(entity.getId());
        dto.setReconciliationResultId(
                entity.getReconciliationResult() != null ? entity.getReconciliationResult().getId() : null);
        dto.setVarianceCode(entity.getVarianceCode());
        dto.setDescription(entity.getDescription());
        dto.setAmount(entity.getAmount());
        dto.setDrCrType(entity.getDrCrType());
        dto.setSourceReference(entity.getSourceReference());
        dto.setTargetReference(entity.getTargetReference());
        dto.setAdjustmentJournalEntryId(
                entity.getAdjustmentJournalEntry() != null ? entity.getAdjustmentJournalEntry().getId() : null);
        dto.setStatus(entity.getStatus());
        dto.setResolvedBy(entity.getResolvedBy());
        dto.setResolvedAt(entity.getResolvedAt());
        return dto;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReconciliationResultId() {
        return reconciliationResultId;
    }

    public void setReconciliationResultId(Long reconciliationResultId) {
        this.reconciliationResultId = reconciliationResultId;
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

    public Long getAdjustmentJournalEntryId() {
        return adjustmentJournalEntryId;
    }

    public void setAdjustmentJournalEntryId(Long adjustmentJournalEntryId) {
        this.adjustmentJournalEntryId = adjustmentJournalEntryId;
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
}
