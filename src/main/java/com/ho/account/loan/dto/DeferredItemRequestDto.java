package com.ho.account.loan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 이연 항목 (DeferredItem) 요청 DTO
 */
@Data
public class DeferredItemRequestDto {
    @NotNull
    private Long loanId;

    @NotNull
    private Long deferredItemTypeId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal amount;

    @NotNull
    private LocalDate deferralDate;

    @NotNull
    private LocalDate amortizationEndDate;

    @NotBlank
    @Size(max = 50)
    private String user;
}
