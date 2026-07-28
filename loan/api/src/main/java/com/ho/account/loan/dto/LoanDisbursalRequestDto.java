package com.ho.account.loan.dto;

// HTTP 요청 계약은 loan:api 인바운드 어댑터가 소유합니다.

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 대출 실행 (Loan Disbursal) 요청 DTO
 */
@Data
public class LoanDisbursalRequestDto {
    @NotNull
    @Positive
    private Long loanId;

    @NotNull
    private LocalDate disbursalDate;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal disbursedAmount;

    @NotBlank
    @Size(max = 50)
    private String user;
}
