package com.ho.account.closing.dto;

import com.ho.account.closing.domain.ValuationBatch;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 평가 배치 (ValuationBatch) 응답 DTO
 */
@Data
@Builder
public class ValuationBatchDto {
    private Long id;
    private Long fiscalPeriodId;
    private String fiscalYear;
    private String fiscalPeriod;
    private ValuationBatch.ValuationType valuationType;
    private LocalDateTime runDateTime;
    private ValuationBatch.ValuationBatchStatus status;
    private Long generatedJournalEntryId;
    private String executionKey;
    private Integer journalCount;
    private boolean financialEffectsPending;
    private String reportLink;
    private String runBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser;

    public static ValuationBatchDto fromEntity(ValuationBatch entity) {
        return ValuationBatchDto.builder()
                .id(entity.getId())
                .fiscalPeriodId(entity.getFiscalPeriodId())
                .fiscalYear(entity.getFiscalYear())
                .fiscalPeriod(entity.getFiscalPeriod())
                .valuationType(entity.getValuationType())
                .runDateTime(entity.getRunDateTime())
                .status(entity.getStatus())
                .generatedJournalEntryId(entity.getGeneratedJournalEntryId())
                .executionKey(entity.getExecutionKey())
                .journalCount(entity.getJournalCount())
                .financialEffectsPending(entity.getStatus() == ValuationBatch.ValuationBatchStatus.RUNNING
                        || entity.getStatus() == ValuationBatch.ValuationBatchStatus.RECONCILIATION_REQUIRED
                        || entity.getStatus() == ValuationBatch.ValuationBatchStatus.PENDING_APPROVAL)
                .reportLink(entity.getReportLink())
                .runBy(entity.getRunBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .auditUser(entity.getAuditUser())
                .build();
    }
}
