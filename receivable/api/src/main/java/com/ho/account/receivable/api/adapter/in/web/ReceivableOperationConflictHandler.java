package com.ho.account.receivable.api.adapter.in.web;

import com.ho.account.receivable.application.service.ReceivableOperationConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Exposes financial operation-key conflicts consistently at receivable HTTP entry points. */
@RestControllerAdvice(assignableTypes = {CollectionController.class, SalesController.class})
public class ReceivableOperationConflictHandler {

    @ExceptionHandler(ReceivableOperationConflictException.class)
    public ResponseEntity<ProblemDetail> handle(ReceivableOperationConflictException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }
}
