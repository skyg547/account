package com.ho.account.contracts.journal;

public record JournalPostingResult(
        Long journalEntryId,
        String slipNo,
        String status) {
}
