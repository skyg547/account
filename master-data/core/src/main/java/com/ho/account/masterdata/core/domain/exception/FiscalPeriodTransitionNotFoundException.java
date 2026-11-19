package com.ho.account.masterdata.core.domain.exception;

/** An operation receipt or its fiscal period cannot be found. */
public final class FiscalPeriodTransitionNotFoundException extends RuntimeException {
    public FiscalPeriodTransitionNotFoundException() {
        super("Fiscal transition not found");
    }
}
