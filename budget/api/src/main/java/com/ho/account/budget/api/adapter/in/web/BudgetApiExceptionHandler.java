package com.ho.account.budget.api.adapter.in.web;

import com.ho.account.budget.api.dto.ApiErrorResponse;
import com.ho.account.budget.application.exception.BudgetConflictException;
import com.ho.account.budget.application.exception.BudgetResourceNotFoundException;
import com.ho.account.budget.application.exception.BudgetRuleViolationException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converts core/application failure meaning into a stable HTTP contract.
 *
 * <p>The core does not depend on HTTP status codes. This inbound adapter owns
 * that translation and avoids returning framework stack details to clients.</p>
 */
@RestControllerAdvice
public class BudgetApiExceptionHandler {

    @ExceptionHandler(BudgetResourceNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleNotFound(BudgetResourceNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "BUDGET_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(BudgetConflictException.class)
    ResponseEntity<ApiErrorResponse> handleConflict(BudgetConflictException exception) {
        return error(HttpStatus.CONFLICT, "BUDGET_CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(BudgetRuleViolationException.class)
    ResponseEntity<ApiErrorResponse> handleRuleViolation(BudgetRuleViolationException exception) {
        return error(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "BUDGET_RULE_VIOLATION",
                exception.getMessage());
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class
    })
    ResponseEntity<ApiErrorResponse> handleInvalidInput(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request validation failed");
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String code,
            String message) {
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(status.value(), code, message));
    }
}
