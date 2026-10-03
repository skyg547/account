package com.ho.account.auth.core.application.exception;

public class DepartmentValidationUnavailableException extends RuntimeException {
    public static final String SAFE_MESSAGE = "Department validation is unavailable";

    public DepartmentValidationUnavailableException() {
        super(SAFE_MESSAGE);
    }
}
