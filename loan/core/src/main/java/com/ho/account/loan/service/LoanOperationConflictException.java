package com.ho.account.loan.service;

/** The stable operation ID is already bound to different source request content. */
public final class LoanOperationConflictException extends IllegalStateException {
    public LoanOperationConflictException() {
        super("Loan operation ID already exists with different request content.");
    }
}
