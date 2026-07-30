package com.ho.account.budget.domain;

/**
 * 예산안은 초안에서 승인되고, 승인된 예산만 마감될 수 있습니다.
 */
public enum BudgetPlanStatus {
    DRAFT,
    APPROVED,
    CLOSED
}
