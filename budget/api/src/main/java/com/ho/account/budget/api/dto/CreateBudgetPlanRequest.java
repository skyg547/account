package com.ho.account.budget.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Transport contract for creating a monthly budget plan.
 *
 * <p>Syntax and numeric shape belong here. Whether the plan may be created is
 * a business decision and therefore remains in the core use case.</p>
 */
public record CreateBudgetPlanRequest(
        @NotBlank @Size(max = 50) String planCode,
        @NotBlank
        @Pattern(
                regexp = "\\d{4}(0[1-9]|1[0-2])",
                message = "yearMonth must use YYYYMM with a valid month")
        String yearMonth,
        @NotBlank @Size(max = 50) String departmentCode,
        @NotBlank @Size(max = 50) String accountCode,
        @NotNull
        @Positive
        @Digits(integer = 17, fraction = 2)
        BigDecimal allocationAmount) {
}
