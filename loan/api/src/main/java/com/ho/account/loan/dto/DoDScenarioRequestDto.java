package com.ho.account.loan.dto;

// HTTP 요청 계약은 loan:api 인바운드 어댑터가 소유합니다.

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DoD 시나리오 재현 요청 DTO
 */
@Data
public class DoDScenarioRequestDto {
    @NotBlank
    @Size(max = 64)
    @jakarta.validation.constraints.Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_-]{0,63}")
    private String operationId;

    @NotNull
    @Positive
    private Long loanId;

    @NotBlank
    @Size(max = 50)
    private String user;
}
