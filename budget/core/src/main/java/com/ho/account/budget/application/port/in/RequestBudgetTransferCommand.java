package com.ho.account.budget.application.port.in;

import com.ho.account.budget.application.exception.BudgetRuleViolationException;
import com.ho.account.budget.domain.BudgetPrecision;
import java.math.BigDecimal;

public record RequestBudgetTransferCommand(
        String requestKey,
        Long sourcePlanId,
        Long targetPlanId,
        BigDecimal amount,
        String actor) {

    public RequestBudgetTransferCommand {
        requestKey = CreateBudgetPlanCommand.requireText(requestKey, "requestKey", 100);
        if (sourcePlanId == null || sourcePlanId <= 0) {
            throw new BudgetRuleViolationException("sourcePlanId는 양수여야 합니다.");
        }
        if (targetPlanId == null || targetPlanId <= 0) {
            throw new BudgetRuleViolationException("targetPlanId는 양수여야 합니다.");
        }
        if (sourcePlanId.equals(targetPlanId)) {
            throw new BudgetRuleViolationException("출발 예산과 도착 예산은 달라야 합니다.");
        }
        try {
            amount = BudgetPrecision.positive(amount, "amount");
        } catch (IllegalArgumentException exception) {
            throw new BudgetRuleViolationException(exception.getMessage(), exception);
        }
        actor = CreateBudgetPlanCommand.requireText(actor, "actor", 80);
    }
}
