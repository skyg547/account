package com.ho.account.budget.application.port.in;

import com.ho.account.budget.application.exception.BudgetRuleViolationException;
import com.ho.account.budget.domain.BudgetPrecision;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExecuteBudgetCommand(
        Long budgetPlanId,
        String sourceType,
        String sourceId,
        String sourceLineId,
        LocalDate executionDate,
        BigDecimal amount,
        String actor) {

    public ExecuteBudgetCommand {
        if (budgetPlanId == null || budgetPlanId <= 0) {
            throw new BudgetRuleViolationException("budgetPlanId는 양수여야 합니다.");
        }
        sourceType = CreateBudgetPlanCommand.requireText(sourceType, "sourceType", 50);
        sourceId = CreateBudgetPlanCommand.requireText(sourceId, "sourceId", 100);
        sourceLineId = CreateBudgetPlanCommand.requireText(sourceLineId, "sourceLineId", 100);
        if (executionDate == null) {
            throw new BudgetRuleViolationException("executionDate은(는) 필수입니다.");
        }
        try {
            amount = BudgetPrecision.positive(amount, "amount");
        } catch (IllegalArgumentException exception) {
            throw new BudgetRuleViolationException(exception.getMessage(), exception);
        }
        actor = CreateBudgetPlanCommand.requireText(actor, "actor", 80);
    }
}
