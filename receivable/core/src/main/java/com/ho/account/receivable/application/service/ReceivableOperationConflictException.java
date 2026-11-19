package com.ho.account.receivable.application.service;

/** A retry reused an operation identity with different financial content. */
public class ReceivableOperationConflictException extends IllegalArgumentException {

    public ReceivableOperationConflictException(String message) {
        super(message);
    }

    public ReceivableOperationConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
