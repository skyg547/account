package com.ho.account.closing.application.service;

/** The durable decision is retained; an operator must reconcile the remote outcome. */
public class ClosingTransitionPendingException extends RuntimeException {
    public ClosingTransitionPendingException(String operationId, RuntimeException cause) {
        super("Fiscal period transition requires recovery; durable operation: " + operationId, cause);
    }
}
