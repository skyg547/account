package com.ho.account.reconciliation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 대사 실행 (ReconciliationRun) 요청 DTO
 */
@Data
public class ReconciliationRunRequestDto {
    @NotNull
    private Long reconciliationUnitId;

    @NotNull
    private LocalDate reconciliationDate;
}
