package com.ho.account.journalledger.domain.journal.service;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.repository.JournalEntryRepository;
import com.ho.account.journalledger.application.service.journal.JournalRuleEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JournalService {

    private final JournalEntryRepository journalEntryRepository;
    private final JournalRuleEngine journalRuleEngine;
    private final AccountSubjectPersistencePort accountSubjectPersistencePort;
    private final DepartmentPersistencePort departmentPersistencePort;
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;

    @Transactional
    public JournalEntry createJournalEntry(JournalEntry journalEntry) {
        journalEntry.setStatus(JournalEntryStatus.DRAFT);
        return journalEntryRepository.save(journalEntry);
    }

    public Optional<JournalEntry> createJournalEntryFromEvent(Map<String, Object> eventData, LocalDate accountingDate) {
        return journalRuleEngine.generateJournalEntry(eventData, accountingDate);
    }

    public List<JournalEntry> getJournalEntriesByDate(LocalDate startDate, LocalDate endDate) {
        return journalEntryRepository.findByAccountingDateBetween(startDate, endDate);
    }

    public Optional<JournalEntry> getJournalEntryBySlipNo(String slipNo) {
        return journalEntryRepository.findBySlipNo(slipNo);
    }

    @Transactional
    public void approveJournalEntry(Long id) {
        JournalEntry entry = journalEntryRepository.findById(id).orElseThrow();
        entry.setStatus(JournalEntryStatus.APPROVED);
        journalEntryRepository.save(entry);
    }

    @Transactional
    public void postJournalEntry(Long id) {
        JournalEntry entry = journalEntryRepository.findById(id).orElseThrow();
        entry.setStatus(JournalEntryStatus.POSTED);
        journalEntryRepository.save(entry);
        // TODO: Ledger 연동 로직 추가
    }
}
