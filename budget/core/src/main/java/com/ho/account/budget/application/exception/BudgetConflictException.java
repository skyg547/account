package com.ho.account.budget.application.exception;

/**
 * 중복 업무키나 멱등성 payload 충돌을 API가 HTTP 409로 변환할 application 예외입니다.
 */
public class BudgetConflictException extends RuntimeException {

    public BudgetConflictException(String message) {
        super(message);
    }

    public BudgetConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
