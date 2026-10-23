package com.ho.account.contracts.journal;

import java.util.Objects;

/** A durable Journal result. Consumers must compare the complete command and expected state. */
public record JournalPeriodFenceResult(JournalPeriodFenceCommand command, State state) {
    public enum State { ACTIVE, RELEASED }

    public JournalPeriodFenceResult {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(state, "state must not be null");
    }
}
