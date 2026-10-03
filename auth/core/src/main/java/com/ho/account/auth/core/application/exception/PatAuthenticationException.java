package com.ho.account.auth.core.application.exception;

public class PatAuthenticationException extends RuntimeException {

    public PatAuthenticationException() {
        super("Authentication required.");
    }
}
