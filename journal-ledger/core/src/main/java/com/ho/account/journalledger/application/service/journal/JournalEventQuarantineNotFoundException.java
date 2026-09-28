package com.ho.account.journalledger.application.service.journal;

/** Typed application failure so only an absent quarantine is mapped to HTTP 404. */
public class JournalEventQuarantineNotFoundException extends RuntimeException {

    public JournalEventQuarantineNotFoundException(Long quarantineId) {
        super("Journal event quarantine not found: " + quarantineId);
    }
}
