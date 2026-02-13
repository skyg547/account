package com.ho.account.loan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
