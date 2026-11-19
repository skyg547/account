package com.ho.account.auth.api.web;

import com.ho.account.auth.api.dto.ErrorResponse;
import com.ho.account.auth.core.application.exception.GovernanceException;
import java.time.LocalDateTime;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(-10)
@RestControllerAdvice(assignableTypes = GovernanceController.class)
public class GovernanceExceptionHandler {
    @ExceptionHandler(GovernanceException.class)
    public ResponseEntity<ErrorResponse> governance(GovernanceException error) {
        int status = switch (error.reason()) {
            case UNAUTHENTICATED -> 401;
            case FORBIDDEN -> 403;
            case NOT_FOUND -> 404;
            case CONFLICT -> 409;
        };
        return response(status, error.reason().name());
    }

    @ExceptionHandler({DataIntegrityViolationException.class, PessimisticLockingFailureException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> conflict(RuntimeException error) { return response(409, "CONFLICT"); }

    @ExceptionHandler({IllegalArgumentException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> invalid(Exception error) { return response(400, "INVALID_REQUEST"); }

    private ResponseEntity<ErrorResponse> response(int status, String code) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, "Governance request denied", LocalDateTime.now()));
    }
}
