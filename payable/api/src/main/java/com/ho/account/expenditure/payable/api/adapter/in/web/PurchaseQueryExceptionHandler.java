package com.ho.account.expenditure.payable.api.adapter.in.web;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Purchase API 조회 예외를 클라이언트가 구분할 수 있는 HTTP 응답으로 변환합니다.
 */
@RestControllerAdvice(assignableTypes = PurchaseController.class)
public class PurchaseQueryExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<PurchaseQueryErrorResponse> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(new PurchaseQueryErrorResponse("INVALID_REQUEST", exception.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<PurchaseQueryErrorResponse> handleNotFound(EntityNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new PurchaseQueryErrorResponse("RESOURCE_NOT_FOUND", exception.getMessage()));
    }

    public record PurchaseQueryErrorResponse(String code, String message) {
    }
}
