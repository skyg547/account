package com.ho.account.auth.core.application.exception;

public class GovernanceException extends RuntimeException {
    public enum Reason { UNAUTHENTICATED, FORBIDDEN, NOT_FOUND, CONFLICT }
    private final Reason reason;

    public GovernanceException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}
