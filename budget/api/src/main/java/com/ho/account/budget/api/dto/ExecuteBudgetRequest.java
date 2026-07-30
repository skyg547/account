package com.ho.account.budget.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** HTTP request mapped to one idempotently identifiable budget execution. */
public record ExecuteBudgetRequest(
        @NotNull @Positive Long planId,
        @NotBlank @Size(max = 50) String sourceType,
        @NotBlank @Size(max = 100) String sourceId,
        @NotBlank @Size(max = 100) String sourceLineId,
        @NotNull
        @Positive
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount,
        @NotNull LocalDate executionDate) {
}
