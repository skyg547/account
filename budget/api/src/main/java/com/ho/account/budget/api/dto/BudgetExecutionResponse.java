package com.ho.account.budget.api.dto;

import com.ho.account.budget.domain.BudgetExecution;
import com.ho.account.budget.domain.BudgetExecutionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Stable HTTP projection of an execution and its cancellation evidence. */
public record BudgetExecutionResponse(
        Long id,
        Long budgetPlanId,
        String sourceType,
        String sourceId,
        String sourceLineId,
        BigDecimal amount,
        LocalDate executionDate,
        BudgetExecutionStatus status,
        String executedBy,
        String cancelledBy) {

    public static BudgetExecutionResponse from(BudgetExecution execution) {
        return new BudgetExecutionResponse(
                execution.id(),
                execution.budgetPlanId(),
                execution.sourceType(),
                execution.sourceId(),
                execution.sourceLineId(),
                execution.amount(),
                execution.executionDate(),
                execution.status(),
                execution.executedBy(),
                execution.cancelledBy());
    }
}
