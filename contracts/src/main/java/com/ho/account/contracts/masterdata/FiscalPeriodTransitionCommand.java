package com.ho.account.contracts.masterdata;

import java.util.Objects;
import java.util.regex.Pattern;

/** Immutable identity and expected-state condition for one authorized fiscal transition. */
public record FiscalPeriodTransitionCommand(
        String operationId,
        Long fiscalPeriodId,
        FiscalPeriodClosingStatus expectedStatus,
        FiscalPeriodClosingStatus targetStatus,
        String auditUser) {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z0-9._@-]+");

    public FiscalPeriodTransitionCommand {
        requireIdentifier(operationId, "operationId", 128);
        if (fiscalPeriodId == null || fiscalPeriodId <= 0) {
            throw new IllegalArgumentException("fiscalPeriodId must be positive");
        }
        Objects.requireNonNull(expectedStatus, "expectedStatus");
        Objects.requireNonNull(targetStatus, "targetStatus");
        if (expectedStatus == targetStatus) {
            throw new IllegalArgumentException("expectedStatus and targetStatus must differ");
        }
        requireIdentifier(auditUser, "auditUser", 42);
    }

    private static void requireIdentifier(String value, String name, int maxLength) {
        if (value == null || value.length() > maxLength || !SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException(name + " must use safe identifier characters and at most "
                    + maxLength + " characters");
        }
    }
}
