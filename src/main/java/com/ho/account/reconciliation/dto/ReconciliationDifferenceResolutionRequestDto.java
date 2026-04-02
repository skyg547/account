package com.ho.account.reconciliation.dto;

import com.ho.account.reconciliation.domain.ReconciliationDifference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 대사 차이 해결 요청 DTO
 */
@Data
public class ReconciliationDifferenceResolutionRequestDto {
    @NotNull
    private Long differenceId;

    @NotNull
    private Long reasonCodeId;

    private Long adjustmentJournalEntryId;

    @NotNull
    private ReconciliationDifference.ReconciliationDifferenceStatus status;

    @NotBlank
    private String resolvedBy;
}
