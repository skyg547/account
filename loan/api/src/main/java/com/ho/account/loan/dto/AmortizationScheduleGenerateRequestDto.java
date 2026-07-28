package com.ho.account.loan.dto;

// HTTP 요청 계약은 loan:api 인바운드 어댑터가 소유합니다.

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 상각 스케줄 생성 요청 (AmortizationScheduleGenerate) DTO
 */
@Data
public class AmortizationScheduleGenerateRequestDto {
    @NotNull
    @Positive
    private Long loanId;

    @NotNull
    private LocalDate recalculationDate; // 최초 생성 시에는 대출 실행일

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @DecimalMax(value = "1.0", inclusive = true)
    private BigDecimal newEIR; // 적용할 새로운 EIR

    @NotBlank
    @Size(max = 50)
    private String user;
}
