package com.ho.account.closing.web;

import com.ho.account.closing.application.service.ClosingTransitionPendingException;
import com.ho.account.closing.application.service.FinalCloseEvidenceValidationException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Closing REST API 컨트롤러의 도메인/비즈니스 예외를 명확한 HTTP 응답 코드로 변환합니다.
 */
@RestControllerAdvice(assignableTypes = {
        ClosingController.class,
        ClosingTransitionController.class,
        FinalCloseEvidenceController.class
})
public class ClosingExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ClosingErrorResponse> handleNotFound(EntityNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ClosingErrorResponse("RESOURCE_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ClosingErrorResponse> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(new ClosingErrorResponse("INVALID_REQUEST", exception.getMessage()));
    }

    @ExceptionHandler(FinalCloseEvidenceValidationException.class)
    public ResponseEntity<ClosingErrorResponse> handleFinalCloseEvidenceBlocked(
            FinalCloseEvidenceValidationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ClosingErrorResponse("FINAL_CLOSE_EVIDENCE_BLOCKED", exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ClosingErrorResponse> handleConflict(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ClosingErrorResponse("WORKFLOW_STATE_CONFLICT", exception.getMessage()));
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ClosingErrorResponse> handleConcurrentMutation(PessimisticLockingFailureException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ClosingErrorResponse("WORKFLOW_STATE_CONFLICT", "Another closing operation holds the required database lock."));
    }

    @ExceptionHandler(ClosingTransitionPendingException.class)
    public ResponseEntity<ClosingErrorResponse> handlePendingTransition(ClosingTransitionPendingException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ClosingErrorResponse("PERIOD_TRANSITION_RECOVERY_REQUIRED", exception.getMessage()));
    }

    public record ClosingErrorResponse(String code, String message) {
    }
}
