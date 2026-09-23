package com.ho.account.receivable.api.adapter.in.web;

import com.ho.account.receivable.application.port.in.SalesUseCase.InvalidSalesInvoiceStatusException;
import com.ho.account.receivable.application.port.in.SalesUseCase.SalesInvoiceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = SalesController.class)
public class SalesQueryExceptionHandler {

    @ExceptionHandler(InvalidSalesInvoiceStatusException.class)
    public ResponseEntity<Void> handleInvalidQuery(InvalidSalesInvoiceStatusException exception) {
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(SalesInvoiceNotFoundException.class)
    public ResponseEntity<Void> handleMissingInvoice(SalesInvoiceNotFoundException exception) {
        return ResponseEntity.notFound().build();
    }
}
