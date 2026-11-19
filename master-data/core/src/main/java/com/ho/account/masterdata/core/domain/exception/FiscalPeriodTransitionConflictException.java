package com.ho.account.masterdata.core.domain.exception;

/** Expected-state, identity or lifecycle conflict for a fiscal transition operation. */
public final class FiscalPeriodTransitionConflictException extends IllegalStateException {
    public FiscalPeriodTransitionConflictException(String message) {
        super(message);
    }
}
