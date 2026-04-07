package com.ho.account.reconciliation.dto;

import com.ho.account.reconciliation.domain.ReconciliationUnit;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 대사 단위 (ReconciliationUnit) 응답 DTO
 */
@Data
@Builder
public class ReconciliationUnitDto {
    private Long id;
    private String name;
    private String description;
    private ReconciliationUnit.ReconciliationFrequency frequency;
    private ReconciliationUnit.ReconciliationType reconciliationType;
    private String criteriaJson;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser;

    public static ReconciliationUnitDto fromEntity(ReconciliationUnit entity) {
        return ReconciliationUnitDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .frequency(entity.getFrequency())
                .reconciliationType(entity.getReconciliationType())
                .criteriaJson(entity.getCriteriaJson())
                .isActive(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .auditUser(entity.getAuditUser())
                .build();
    }
}
