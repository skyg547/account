package com.ho.account.asset.domain;

/** A caller reused one business event identifier with different submitted terms. */
public class LeaseRemeasurementConflictException extends IllegalStateException {
    public LeaseRemeasurementConflictException() {
        super("remeasurement operationId already has a different request");
    }
}
