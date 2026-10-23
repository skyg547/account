package com.ho.account.auth.core.application.exception;

public class PatAccessDeniedException extends RuntimeException {

    public PatAccessDeniedException() {
        super("Administrator authority required.");
    }
}
