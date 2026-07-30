package com.ho.account.budget.application.exception;

/**
 * 형식은 유효하지만 업무 규칙을 위반한 요청을 API가 HTTP 422로 변환할 예외입니다.
 */
public class BudgetRuleViolationException extends RuntimeException {

    public BudgetRuleViolationException(String message) {
        super(message);
    }

    public BudgetRuleViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
