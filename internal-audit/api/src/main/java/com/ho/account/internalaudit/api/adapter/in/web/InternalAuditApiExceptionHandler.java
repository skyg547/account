package com.ho.account.internalaudit.api.adapter.in.web;

import com.ho.account.internalaudit.core.domain.IdempotencyConflictException;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {RcmController.class, EvaluationController.class})
public class InternalAuditApiExceptionHandler {

    @ExceptionHandler(IdempotencyConflictException.class)
    ResponseEntity<Map<String, String>> idempotencyConflict(IdempotencyConflictException exception) {
        // A conflict must not reveal the stored command, actor, or result snapshot.
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "code", "IDEMPOTENCY_CONFLICT",
                        "message", "Idempotency key cannot be reused for this command."));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, String>> internalFailure(IllegalStateException exception) {
        // Receipt and serialization failures may contain internal state in their causes.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "code", "INTERNAL_AUDIT_ERROR",
                        "message", "Unable to process the command."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> invalidRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Map<String, String>> notFound(NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", exception.getMessage()));
    }
}
