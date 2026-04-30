package com.ho.account.closing.dto;

import com.ho.account.closing.domain.ClosingAdjustment;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 결산 조정 (ClosingAdjustment) 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClosingAdjustmentDto {
    private Long id;
    private Long fiscalPeriodId;
    private String fiscalYear;
    private String fiscalPeriod;
    private Long journalEntryId;
    private ClosingAdjustment.AdjustmentType adjustmentType;
    private String description;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser;

    public static ClosingAdjustmentDto fromEntity(ClosingAdjustment entity) {
        return ClosingAdjustmentDto.builder()
                .id(entity.getId())
                .fiscalPeriodId(entity.getFiscalPeriod() != null ? entity.getFiscalPeriod().getId() : null)
                .fiscalYear(entity.getFiscalPeriod() != null ? entity.getFiscalPeriod().getFiscalYear() : null)
                .fiscalPeriod(entity.getFiscalPeriod() != null ? entity.getFiscalPeriod().getFiscalPeriod() : null)
                .journalEntryId(entity.getJournalEntryId())
                .adjustmentType(entity.getAdjustmentType())
                .description(entity.getDescription())
                .approvedBy(entity.getApprovedBy())
                .approvedAt(entity.getApprovedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .auditUser(entity.getAuditUser())
                .build();
    }
}
