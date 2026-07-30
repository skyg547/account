package com.ho.account.budget.domain;

/**
 * 회계연도 제어 행의 상태입니다. CLOSED는 되돌릴 수 없는 terminal 상태입니다.
 */
public enum BudgetFiscalYearStatus {
    OPEN,
    CLOSED
}
