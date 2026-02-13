package com.ho.account.closing.dto;

import com.ho.account.closing.domain.ClosingAdjustment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 결산 조정 (ClosingAdjustment) 요청 DTO
 */
@Data
public class ClosingAdjustmentRequestDto {
    @NotNull
    private Long fiscalPeriodId;

    @NotNull
    private Long journalEntryId;

    @NotNull
    private ClosingAdjustment.AdjustmentType adjustmentType;

    @Size(max = 1000)
    private String description;

    @NotBlank
    @Size(max = 50)
    private String approvedBy;
}
