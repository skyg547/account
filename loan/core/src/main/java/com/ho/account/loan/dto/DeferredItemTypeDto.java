package com.ho.account.loan.dto;

import com.ho.account.loan.domain.DeferredItemType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 이연 항목 유형 (DeferredItemType) 응답 DTO
 */
@Data
@Builder
public class DeferredItemTypeDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private DeferredItemType.DeferralMethod deferralMethod;
    private DeferredItemType.EirCashFlowTreatment eirCashFlowTreatment;
    private String deferredAssetAccountCode;
    private String deferredAssetAccountName;
    private String recognizedIncomeAccountCode;
    private String recognizedIncomeAccountName;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser;

    public static DeferredItemTypeDto fromEntity(DeferredItemType entity) {
        return DeferredItemTypeDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .deferralMethod(entity.getDeferralMethod())
                .eirCashFlowTreatment(entity.getEirCashFlowTreatment())
                .deferredAssetAccountCode(entity.getDeferredAssetAccount() != null ? entity.getDeferredAssetAccount().getCode() : null)
                .deferredAssetAccountName(entity.getDeferredAssetAccount() != null ? entity.getDeferredAssetAccount().getName() : null)
                .recognizedIncomeAccountCode(entity.getRecognizedIncomeAccount() != null ? entity.getRecognizedIncomeAccount().getCode() : null)
                .recognizedIncomeAccountName(entity.getRecognizedIncomeAccount() != null ? entity.getRecognizedIncomeAccount().getName() : null)
                .isActive(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .auditUser(entity.getAuditUser())
                .build();
    }
}
