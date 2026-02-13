package com.ho.account.closing.dto;

import com.ho.account.closing.domain.ProvisionBatch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 충당/손상 배치 (ProvisionBatch) 요청 DTO
 */
@Data
public class ProvisionBatchRequestDto {
    @NotNull
    private Long fiscalPeriodId;

    @NotNull
    private ProvisionBatch.ProvisionType provisionType;

    @NotBlank
    @Size(max = 50)
    private String runBy;
}
