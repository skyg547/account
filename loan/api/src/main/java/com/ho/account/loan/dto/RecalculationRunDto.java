package com.ho.account.loan.dto;

// HTTP 응답 계약은 loan:api 인바운드 어댑터가 소유합니다.

import com.ho.account.loan.domain.RecalculationRun;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 재계산 실행 (RecalculationRun) 응답 DTO
 */
@Data
@Builder
public class RecalculationRunDto {
    private Long id;
    private Long loanId;
    private String loanNumber;
    private LocalDate recalculationDate;
    private RecalculationRun.RecalculationReason reason;
    private BigDecimal oldEIR;
    private BigDecimal newEIR;
    private LocalDate oldMaturityDate;
    private LocalDate newMaturityDate;
    private Long recalculatedAmortizationScheduleStartId;
    private String impactAnalysis;
    private Long adjustmentJournalEntryId;
    private String adjustmentJournalEntrySlipNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser;

    public static RecalculationRunDto fromEntity(RecalculationRun entity) {
        return RecalculationRunDto.builder()
                .id(entity.getId())
                .loanId(entity.getLoan() != null ? entity.getLoan().getId() : null)
                .loanNumber(entity.getLoan() != null ? entity.getLoan().getLoanNumber() : null)
                .recalculationDate(entity.getRecalculationDate())
                .reason(entity.getReason())
                .oldEIR(entity.getOldEIR())
                .newEIR(entity.getNewEIR())
                .oldMaturityDate(entity.getOldMaturityDate())
                .newMaturityDate(entity.getNewMaturityDate())
                .recalculatedAmortizationScheduleStartId(entity.getRecalculatedAmortizationScheduleStart() != null ? entity.getRecalculatedAmortizationScheduleStart().getId() : null)
                .impactAnalysis(entity.getImpactAnalysis())
                .adjustmentJournalEntryId(entity.getAdjustmentJournalEntryId())
                .adjustmentJournalEntrySlipNo(entity.getAdjustmentJournalEntrySlipNo())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .auditUser(entity.getAuditUser())
                .build();
    }
}
