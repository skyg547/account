package com.ho.account.journalledger.domain.journal.domain;

/** Durable completeness disposition for a Kafka economic event that matched no journal rule. */
public enum JournalEventQuarantineStatus {
    QUARANTINED,
    REPLAYED
}
