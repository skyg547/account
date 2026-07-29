package com.ho.account.closing.web;

import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * EOD API의 도메인 실패를 안정적인 HTTP 의미로 변환합니다.
 */
@RestControllerAdvice(assignableTypes = EodLifecycleController.class)
public class EodLifecycleExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<EodErrorResponse> notFound(NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new EodErrorResponse("EOD_STATUS_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<EodErrorResponse> invalidRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(new EodErrorResponse("EOD_REQUEST_INVALID", exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<EodErrorResponse> lifecycleConflict(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new EodErrorResponse("EOD_STATE_CONFLICT", exception.getMessage()));
    }

    public record EodErrorResponse(String code, String message) {
    }
}
