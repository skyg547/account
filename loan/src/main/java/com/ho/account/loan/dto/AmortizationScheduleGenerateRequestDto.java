package com.ho.account.loan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private Long loanId;

    @NotNull
    private LocalDate recalculationDate; // 최초 생성 시에는 대출 실행일

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal newEIR; // 적용할 새로운 EIR

    @NotBlank
    @Size(max = 50)
    private String user;
}
