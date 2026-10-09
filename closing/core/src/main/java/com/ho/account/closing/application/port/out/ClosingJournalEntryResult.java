package com.ho.account.closing.application.port.out;

public record ClosingJournalEntryResult(Long journalEntryId, String slipNo, String status) {
    public ClosingJournalEntryResult(Long journalEntryId, String slipNo) {
        this(journalEntryId, slipNo, "DRAFT");
    }
}
