package com.ho.account.budget.application.exception;

/**
 * 요청한 예산 자원이 없을 때 API가 HTTP 404로 변환할 application 예외입니다.
 */
public class BudgetResourceNotFoundException extends RuntimeException {

    public BudgetResourceNotFoundException(String message) {
        super(message);
    }
}
