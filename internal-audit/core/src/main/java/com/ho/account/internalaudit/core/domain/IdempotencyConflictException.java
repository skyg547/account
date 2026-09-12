package com.ho.account.internalaudit.core.domain;

/** A key already belongs to another command or an unverifiable legacy audit event. */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException() {
        super("Idempotency key cannot be reused for this command.");
    }
}
