package com.ho.account.closing.dto;

import com.ho.account.closing.domain.ProvisionBatch;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 충당/손상 배치 (ProvisionBatch) 응답 DTO
 */
@Data
@Builder
public class ProvisionBatchDto {
    private Long id;
    private Long fiscalPeriodId;
    private String fiscalYear;
    private String fiscalPeriod;
    private ProvisionBatch.ProvisionType provisionType;
    private LocalDateTime runDateTime;
    private ProvisionBatch.ProvisionBatchStatus status;
    private Long generatedJournalEntryId;
    private String reportLink;
    private String runBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser;

    public static ProvisionBatchDto fromEntity(ProvisionBatch entity) {
        return ProvisionBatchDto.builder()
                .id(entity.getId())
                .fiscalPeriodId(entity.getFiscalPeriodId())
                .fiscalYear(entity.getFiscalYear())
                .fiscalPeriod(entity.getFiscalPeriod())
                .provisionType(entity.getProvisionType())
                .runDateTime(entity.getRunDateTime())
                .status(entity.getStatus())
                .generatedJournalEntryId(entity.getGeneratedJournalEntryId())
                .reportLink(entity.getReportLink())
                .runBy(entity.getRunBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .auditUser(entity.getAuditUser())
                .build();
    }
}
