package com.ho.account.journalledger.application.service.journal;

/** Raised when an existing operation identity is retried with different financial/audit content. */
public class JournalCreationConflictException extends IllegalStateException {

    public JournalCreationConflictException() {
        // Do not include the digest or request content: conflict handling must not leak payload data.
        super("Journal creation identity is already linked to different request content.");
    }
}
