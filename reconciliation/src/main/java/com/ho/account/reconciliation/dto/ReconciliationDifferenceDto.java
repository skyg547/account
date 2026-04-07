package com.ho.account.reconciliation.dto;

import com.ho.account.reconciliation.domain.ReconciliationDifference;
import com.ho.account.reconciliation.domain.ReconciliationDifference.DifferenceType;
import com.ho.account.reconciliation.domain.ReconciliationDifference.ReconciliationDifferenceStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 대사 차이 (ReconciliationDifference) 응답 DTO
 */
@Data
@Builder
public class ReconciliationDifferenceDto {
    private Long id;
    private Long reconciliationRunId;
    private DifferenceType differenceType;
    private BigDecimal amountExpected;
    private BigDecimal amountActual;
    private BigDecimal differenceAmount;
    private String description;
    private String sourceItemRef;
    private String targetItemRef;
    private Long reasonCodeId;
    private String reasonCodeName;
    private Long adjustmentJournalEntryId;
    private ReconciliationDifferenceStatus status;
    private String assignedToUser;
    private LocalDateTime slaDueDate;
    private LocalDateTime resolvedAt;
    private String resolvedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser;

    public static ReconciliationDifferenceDto fromEntity(ReconciliationDifference entity) {
        return ReconciliationDifferenceDto.builder()
                .id(entity.getId())
                .reconciliationRunId(entity.getReconciliationRun() != null ? entity.getReconciliationRun().getId() : null)
                .differenceType(entity.getDifferenceType())
                .amountExpected(entity.getAmountExpected())
                .amountActual(entity.getAmountActual())
                .differenceAmount(entity.getDifferenceAmount())
                .description(entity.getDescription())
                .sourceItemRef(entity.getSourceItemRef())
                .targetItemRef(entity.getTargetItemRef())
                .reasonCodeId(entity.getReasonCode() != null ? entity.getReasonCode().getId() : null)
                .reasonCodeName(entity.getReasonCode() != null ? entity.getReasonCode().getName() : null)
                .adjustmentJournalEntryId(entity.getAdjustmentJournalEntry() != null ? entity.getAdjustmentJournalEntry().getId() : null)
                .status(entity.getStatus())
                .assignedToUser(entity.getAssignedToUser())
                .slaDueDate(entity.getSlaDueDate())
                .resolvedAt(entity.getResolvedAt())
                .resolvedBy(entity.getResolvedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .auditUser(entity.getAuditUser())
                .build();
    }
}
