package com.ho.account.contracts.journal;

import java.util.Objects;

/** Immutable command identity; every retry must retain all three fields exactly. */
public record JournalPeriodFenceCommand(
        String operationId, JournalPeriodCheckpoint checkpoint, String actor) {
    public JournalPeriodFenceCommand {
        operationId = requireText(operationId, "operationId", 100);
        Objects.requireNonNull(checkpoint, "checkpoint must not be null");
        actor = requireText(actor, "actor", 50);
    }

    private static String requireText(String value, String field, int maximum) {
        if (value == null || value.isBlank() || value.length() > maximum) {
            throw new IllegalArgumentException(field + " must contain 1 to " + maximum + " characters");
        }
        // Reject ambiguous identities rather than silently rewriting a retry's immutable payload.
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " must not have surrounding whitespace");
        }
        return value;
    }
}
