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
    private String reportLink;
    private String runBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser;

    public static ValuationBatchDto fromEntity(ValuationBatch entity) {
        return ValuationBatchDto.builder()
                .id(entity.getId())
                .fiscalPeriodId(entity.getFiscalPeriod() != null ? entity.getFiscalPeriod().getId() : null)
                .fiscalYear(entity.getFiscalPeriod() != null ? entity.getFiscalPeriod().getFiscalYear() : null)
                .fiscalPeriod(entity.getFiscalPeriod() != null ? entity.getFiscalPeriod().getFiscalPeriod() : null)
                .valuationType(entity.getValuationType())
                .runDateTime(entity.getRunDateTime())
                .status(entity.getStatus())
                .generatedJournalEntryId(entity.getGeneratedJournalEntry() != null ? entity.getGeneratedJournalEntry().getId() : null)
                .reportLink(entity.getReportLink())
                .runBy(entity.getRunBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .auditUser(entity.getAuditUser())
                .build();
    }
}
