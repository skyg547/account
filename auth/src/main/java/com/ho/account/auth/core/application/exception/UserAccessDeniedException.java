package com.ho.account.auth.core.application.exception;

public class UserAccessDeniedException extends RuntimeException {

    public UserAccessDeniedException(String message) {
        super(message);
    }
}

