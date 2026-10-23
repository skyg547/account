package com.ho.account.expenditure.payable.api.adapter.in.web;

import com.ho.account.expenditure.application.service.PayableOperationConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = PaymentController.class)
public class PaymentConflictExceptionHandler {
    @ExceptionHandler(PayableOperationConflictException.class)
    public ResponseEntity<ConflictResponse> handleConflict(PayableOperationConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ConflictResponse("OPERATION_CONFLICT", exception.getMessage()));
    }

    public record ConflictResponse(String code, String message) {
    }
}
