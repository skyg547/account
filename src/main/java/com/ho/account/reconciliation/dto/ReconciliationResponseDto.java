package com.ho.account.reconciliation.dto;

import com.ho.account.reconciliation.domain.ReconciliationResult;
import com.ho.account.reconciliation.domain.ReconciliationStatus;
import com.ho.account.reconciliation.domain.ReconciliationType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ReconciliationResponseDto {

    private Long id;
    private LocalDate reconciliationDate;
    private ReconciliationType reconciliationType;
    private Long closingPeriodId;
    private ReconciliationStatus status;
    private Long totalCountSource;
    private BigDecimal totalAmountSource;
    private Long totalCountTarget;
    private BigDecimal totalAmountTarget;
    private Long varianceCount;
    private BigDecimal varianceAmount;
    private String runBy;
    private LocalDateTime runAt;
    private List<VarianceDto> variances;

    // Static factory method from entity
    public static ReconciliationResponseDto from(ReconciliationResult entity) {
        ReconciliationResponseDto dto = new ReconciliationResponseDto();
        dto.setId(entity.getId());
        dto.setReconciliationDate(entity.getReconciliationDate());
        dto.setReconciliationType(entity.getReconciliationType());
        dto.setClosingPeriodId(entity.getClosingPeriodId());
        dto.setStatus(entity.getStatus());
        dto.setTotalCountSource(entity.getTotalCountSource());
        dto.setTotalAmountSource(entity.getTotalAmountSource());
        dto.setTotalCountTarget(entity.getTotalCountTarget());
        dto.setTotalAmountTarget(entity.getTotalAmountTarget());
        dto.setVarianceCount(entity.getVarianceCount());
        dto.setVarianceAmount(entity.getVarianceAmount());
        dto.setRunBy(entity.getRunBy());
        dto.setRunAt(entity.getRunAt());
        return dto;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getReconciliationDate() {
        return reconciliationDate;
    }

    public void setReconciliationDate(LocalDate reconciliationDate) {
        this.reconciliationDate = reconciliationDate;
    }

    public ReconciliationType getReconciliationType() {
        return reconciliationType;
    }

    public void setReconciliationType(ReconciliationType reconciliationType) {
        this.reconciliationType = reconciliationType;
    }

    public Long getClosingPeriodId() {
        return closingPeriodId;
    }

    public void setClosingPeriodId(Long closingPeriodId) {
        this.closingPeriodId = closingPeriodId;
    }

    public ReconciliationStatus getStatus() {
        return status;
    }

    public void setStatus(ReconciliationStatus status) {
        this.status = status;
    }

    public Long getTotalCountSource() {
        return totalCountSource;
    }

    public void setTotalCountSource(Long totalCountSource) {
        this.totalCountSource = totalCountSource;
    }

    public BigDecimal getTotalAmountSource() {
        return totalAmountSource;
    }

    public void setTotalAmountSource(BigDecimal totalAmountSource) {
        this.totalAmountSource = totalAmountSource;
    }

    public Long getTotalCountTarget() {
        return totalCountTarget;
    }

    public void setTotalCountTarget(Long totalCountTarget) {
        this.totalCountTarget = totalCountTarget;
    }

    public BigDecimal getTotalAmountTarget() {
        return totalAmountTarget;
    }

    public void setTotalAmountTarget(BigDecimal totalAmountTarget) {
        this.totalAmountTarget = totalAmountTarget;
    }

    public Long getVarianceCount() {
        return varianceCount;
    }

    public void setVarianceCount(Long varianceCount) {
        this.varianceCount = varianceCount;
    }

    public BigDecimal getVarianceAmount() {
        return varianceAmount;
    }

    public void setVarianceAmount(BigDecimal varianceAmount) {
        this.varianceAmount = varianceAmount;
    }

    public String getRunBy() {
        return runBy;
    }

    public void setRunBy(String runBy) {
        this.runBy = runBy;
    }

    public LocalDateTime getRunAt() {
        return runAt;
    }

    public void setRunAt(LocalDateTime runAt) {
        this.runAt = runAt;
    }

    public List<VarianceDto> getVariances() {
        return variances;
    }

    public void setVariances(List<VarianceDto> variances) {
        this.variances = variances;
    }
}
