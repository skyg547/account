package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.repository.JournalEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JournalPersistenceAdapter implements JournalPersistencePort {

    private final JournalEntryRepository journalEntryRepository;

    @Override
    public JournalEntry save(JournalEntry journalEntry) {
        return journalEntryRepository.save(journalEntry);
    }

    @Override
    public Optional<JournalEntry> findById(Long id) {
        return journalEntryRepository.findById(id);
    }

    @Override
    public Optional<JournalEntry> findByIdWithDetails(Long id) {
        return journalEntryRepository.findByIdWithDetails(id);
    }

    @Override
    public Optional<JournalEntry> findBySlipNo(String slipNo) {
        return journalEntryRepository.findBySlipNo(slipNo);
    }

    @Override
    public List<JournalEntry> findByAccountingDateBetween(LocalDate startDate, LocalDate endDate) {
        return journalEntryRepository.findByAccountingDateBetween(startDate, endDate);
    }
}
