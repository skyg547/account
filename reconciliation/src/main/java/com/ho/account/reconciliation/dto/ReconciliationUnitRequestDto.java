package com.ho.account.reconciliation.dto;

import com.ho.account.reconciliation.domain.ReconciliationUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 대사 단위 (ReconciliationUnit) 요청 DTO
 */
@Data
public class ReconciliationUnitRequestDto {
    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull
    private ReconciliationUnit.ReconciliationFrequency frequency;

    @NotNull
    private ReconciliationUnit.ReconciliationType reconciliationType;

    private String criteriaJson; // JSON string for flexible criteria

    private boolean isActive = true;

    // Entity to DTO conversion is not typically needed for a request DTO,
    // but a method to convert DTO to Entity can be useful.
    public ReconciliationUnit toEntity() {
        ReconciliationUnit unit = new ReconciliationUnit();
        unit.setName(this.name);
        unit.setDescription(this.description);
        unit.setFrequency(this.frequency);
        unit.setReconciliationType(this.reconciliationType);
        unit.setCriteriaJson(this.criteriaJson);
        unit.setActive(this.isActive);
        return unit;
    }
}
