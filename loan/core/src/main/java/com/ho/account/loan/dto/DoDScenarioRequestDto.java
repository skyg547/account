package com.ho.account.loan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DoD 시나리오 재현 요청 DTO
 */
@Data
public class DoDScenarioRequestDto {
    @NotNull
    private Long loanId;

    @NotBlank
    @Size(max = 50)
    private String user;
}
