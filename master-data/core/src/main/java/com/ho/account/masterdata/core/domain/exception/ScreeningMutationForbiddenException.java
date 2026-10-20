package com.ho.account.masterdata.core.domain.exception;

/** A generic master-data command attempted to change reviewer-owned screening labels. */
public class ScreeningMutationForbiddenException extends IllegalArgumentException {

    public ScreeningMutationForbiddenException(String message) {
        super(message);
    }
}
