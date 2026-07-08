package com.ho.account.reconciliation.api.dto;

import com.ho.account.reconciliation.domain.DifferenceReasonCode;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 차이 사유 코드 (DifferenceReasonCode) 응답 DTO
 */
@Data
@Builder
public class DifferenceReasonCodeDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private boolean isAdjustable;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser;

    public static DifferenceReasonCodeDto fromEntity(DifferenceReasonCode entity) {
        return DifferenceReasonCodeDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .isAdjustable(entity.isAdjustable())
                .isActive(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .auditUser(entity.getAuditUser())
                .build();
    }
}
