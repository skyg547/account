package com.ho.account.journalledger.application.port.in;

import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface JournalUseCase {
    JournalEntry createJournalEntry(JournalEntry journalEntry);
    Optional<JournalEntry> createJournalEntryFromEvent(Map<String, Object> eventData, LocalDate accountingDate);
    List<JournalEntry> getJournalEntriesByDate(LocalDate startDate, LocalDate endDate);
    Optional<JournalEntry> getJournalEntryBySlipNo(String slipNo);
    Optional<JournalEntry> getJournalEntryWithDetails(Long id);
    void approveJournalEntry(Long id, String approver);
    void postJournalEntry(Long id, String poster);
}
