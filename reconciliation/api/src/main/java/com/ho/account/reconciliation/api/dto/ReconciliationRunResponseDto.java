package com.ho.account.reconciliation.api.dto;

import com.ho.account.reconciliation.domain.ReconciliationRun;
import com.ho.account.reconciliation.domain.ReconciliationRun.ReconciliationRunStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 대사 실행 (ReconciliationRun) 응답 DTO
 */
@Data
@Builder
public class ReconciliationRunResponseDto {
    private Long id;
    private Long reconciliationUnitId;
    private String reconciliationUnitName;
    private LocalDate reconciliationDate;
    private LocalDateTime runStartTime;
    private LocalDateTime runEndTime;
    private ReconciliationRunStatus status;
    private Long totalItemsSource;
    private BigDecimal totalAmountSource;
    private Long totalItemsTarget;
    private BigDecimal totalAmountTarget;
    private Long matchedItemsCount;
    private BigDecimal matchedAmount;
    private Long unmatchedItemsCount;
    private BigDecimal unmatchedAmount;
    private String runBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser;

    public static ReconciliationRunResponseDto fromEntity(ReconciliationRun entity) {
        return ReconciliationRunResponseDto.builder()
                .id(entity.getId())
                .reconciliationUnitId(entity.getReconciliationUnit() != null ? entity.getReconciliationUnit().getId() : null)
                .reconciliationUnitName(entity.getReconciliationUnit() != null ? entity.getReconciliationUnit().getName() : null)
                .reconciliationDate(entity.getReconciliationDate())
                .runStartTime(entity.getRunStartTime())
                .runEndTime(entity.getRunEndTime())
                .status(entity.getStatus())
                .totalItemsSource(entity.getTotalItemsSource())
                .totalAmountSource(entity.getTotalAmountSource())
                .totalItemsTarget(entity.getTotalItemsTarget())
                .totalAmountTarget(entity.getTotalAmountTarget())
                .matchedItemsCount(entity.getMatchedItemsCount())
                .matchedAmount(entity.getMatchedAmount())
                .unmatchedItemsCount(entity.getUnmatchedItemsCount())
                .unmatchedAmount(entity.getUnmatchedAmount())
                .runBy(entity.getRunBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .auditUser(entity.getAuditUser())
                .build();
    }
}
