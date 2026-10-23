package com.ho.account.contracts.journal;

import java.util.Objects;

/**
 * Journal's committed revision obtained BEFORE opening the evidence source snapshots.
 * This is an optimistic token, not permission to close: only a successful fence reserves admission.
 */
public record JournalPeriodCheckpoint(JournalPeriodScope scope, long revision) {
    public JournalPeriodCheckpoint {
        Objects.requireNonNull(scope, "scope must not be null");
        if (revision < 0) {
            throw new IllegalArgumentException("revision must not be negative");
        }
    }
}
