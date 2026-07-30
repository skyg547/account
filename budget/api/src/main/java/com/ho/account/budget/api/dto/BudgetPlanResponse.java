package com.ho.account.budget.api.dto;

import com.ho.account.budget.domain.BudgetPlan;
import com.ho.account.budget.domain.BudgetPlanStatus;
import java.math.BigDecimal;

/** Stable HTTP projection; the domain aggregate itself is never serialized. */
public record BudgetPlanResponse(
        Long id,
        String planCode,
        String yearMonth,
        String departmentCode,
        String accountCode,
        BigDecimal allocatedAmount,
        BigDecimal transferInAmount,
        BigDecimal transferOutAmount,
        BigDecimal executedAmount,
        BigDecimal availableAmount,
        BudgetPlanStatus status,
        String createdBy,
        String approvedBy,
        String closedBy) {

    public static BudgetPlanResponse from(BudgetPlan plan) {
        return new BudgetPlanResponse(
                plan.id(),
                plan.planCode(),
                plan.yearMonth(),
                plan.departmentCode(),
                plan.accountCode(),
                plan.allocatedAmount(),
                plan.transferInAmount(),
                plan.transferOutAmount(),
                plan.executedAmount(),
                plan.availableAmount(),
                plan.status(),
                plan.createdBy(),
                plan.approvedBy(),
                plan.closedBy());
    }
}
