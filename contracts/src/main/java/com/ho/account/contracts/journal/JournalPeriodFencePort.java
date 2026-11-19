package com.ho.account.contracts.journal;

/**
 * Admission protocol against ONE authoritative Journal database, in-process or over transport.
 * Implementations return only after their independent Journal transaction commits.
 * A transport timeout is an unknown outcome, never an implicit release or success.
 */
public interface JournalPeriodFencePort {
    /** Drain earlier monetary transactions, then obtain a token before fresh primary-source reads. */
    JournalPeriodCheckpoint checkpoint(JournalPeriodScope scope);

    /**
     * Drain in-flight monetary transactions and atomically compare the checkpoint revision before
     * reserving admission. An identical ACTIVE command returns its stored receipt; conflicting
     * payloads, changed revisions, and released operations fail closed.
     */
    JournalPeriodFenceResult fence(JournalPeriodFenceCommand command);

    /**
     * Release only this immutable operation, recording a tombstone even if its fence has not arrived.
     * Repeated release is idempotent. A delayed fence must never resurrect a released operation.
     * The first release advances the Journal revision so pre-reopen evidence cannot be reused.
     */
    JournalPeriodFenceResult release(JournalPeriodFenceCommand command);
}
