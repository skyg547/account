package com.ho.account.closing.dto;

import com.ho.account.closing.domain.ValuationBatch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 평가 배치 (ValuationBatch) 요청 DTO
 */
@Data
public class ValuationBatchRequestDto {
    @NotNull
    private Long fiscalPeriodId;

    @NotNull
    private ValuationBatch.ValuationType valuationType;

    @NotBlank
    @Size(max = 50)
    private String runBy;
}
