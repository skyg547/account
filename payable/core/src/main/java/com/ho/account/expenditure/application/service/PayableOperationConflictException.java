package com.ho.account.expenditure.application.service;

/** A supplied operation identity already belongs to different financial content. */
public class PayableOperationConflictException extends RuntimeException {
    public PayableOperationConflictException(String message) {
        super(message);
    }

    public PayableOperationConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
