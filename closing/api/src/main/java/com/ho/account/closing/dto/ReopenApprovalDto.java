package com.ho.account.closing.dto;

import com.ho.account.closing.domain.ReopenApproval;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 기간 재오픈 승인 (ReopenApproval) 응답 DTO
 */
@Data
@Builder
public class ReopenApprovalDto {
    private Long id;
    private Long fiscalPeriodId;
    private String fiscalYear;
    private String fiscalPeriod;
    private String requestedBy;
    private LocalDateTime requestedAt;
    private String reason;
    private ReopenApproval.ReopenApprovalStatus status;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String impactAnalysisReport;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser;

    public static ReopenApprovalDto fromEntity(ReopenApproval entity) {
        return ReopenApprovalDto.builder()
                .id(entity.getId())
                .fiscalPeriodId(entity.getFiscalPeriodId())
                .fiscalYear(entity.getFiscalYear())
                .fiscalPeriod(entity.getFiscalPeriod())
                .requestedBy(entity.getRequestedBy())
                .requestedAt(entity.getRequestedAt())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .approvedBy(entity.getApprovedBy())
                .approvedAt(entity.getApprovedAt())
                .impactAnalysisReport(entity.getImpactAnalysisReport())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .auditUser(entity.getAuditUser())
                .build();
    }
}
