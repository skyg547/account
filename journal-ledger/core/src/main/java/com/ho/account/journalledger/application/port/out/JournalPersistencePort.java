package com.ho.account.journalledger.application.port.out;

import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JournalPersistencePort {
    JournalEntry save(JournalEntry journalEntry);
    Optional<JournalEntry> findById(Long id);
    Optional<JournalEntry> findByIdWithDetails(Long id);
    Optional<JournalEntry> findBySlipNo(String slipNo);
    List<JournalEntry> findByAccountingDateBetween(LocalDate startDate, LocalDate endDate);
}
