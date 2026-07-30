package com.ho.account.budget.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** HTTP request mapped to the core transfer command. */
public record RequestBudgetTransferRequest(
        @NotBlank @Size(max = 100) String requestKey,
        @NotNull @Positive Long fromPlanId,
        @NotNull @Positive Long toPlanId,
        @NotNull
        @Positive
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount) {
}
