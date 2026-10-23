package com.ho.account.closing.application.service;

/** Final close is fenced until a complete, current and internally consistent evidence set exists. */
public class FinalCloseEvidenceValidationException extends IllegalStateException {

    public FinalCloseEvidenceValidationException(String message) {
        super("Final close evidence rejected: " + message);
    }
}
