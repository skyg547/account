package com.ho.account.contracts.journal;

public interface JournalPostingPort {

    JournalPostingResult createDraftEntry(JournalEntryCommand command);
}
