package com.ho.account.journalledger.application.service.journal;

import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 전표 애플리케이션 서비스
 * 헥사고날 아키텍처의 Inbound Port(JournalUseCase)를 구현합니다.
 */
@Service
@RequiredArgsConstructor
public class JournalEntryService implements JournalUseCase {

    private final JournalPersistencePort journalPersistencePort;
    private final JournalRuleEngine journalRuleEngine;

    @Override
    @Transactional
    public JournalEntry createJournalEntry(JournalEntry journalEntry) {
        // 엔티티의 validateBalance 호출로 정합성 검증 강제
        journalEntry.validateBalance();
        return journalPersistencePort.save(journalEntry);
    }

    @Override
    public Optional<JournalEntry> createJournalEntryFromEvent(Map<String, Object> eventData, LocalDate accountingDate) {
        return journalRuleEngine.generateJournalEntry(eventData, accountingDate);
    }

    @Override
    public List<JournalEntry> getJournalEntriesByDate(LocalDate startDate, LocalDate endDate) {
        return journalPersistencePort.findByAccountingDateBetween(startDate, endDate);
    }

    @Override
    public Optional<JournalEntry> getJournalEntryBySlipNo(String slipNo) {
        return journalPersistencePort.findBySlipNo(slipNo);
    }

    @Override
    public Optional<JournalEntry> getJournalEntryWithDetails(Long id) {
        return journalPersistencePort.findByIdWithDetails(id);
    }

    @Override
    @Transactional
    public void approveJournalEntry(Long id, String approver) {
        JournalEntry entry = journalPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 전표입니다: " + id));
        
        // 도메인 엔티티에 위임 (Rich Domain Model)
        entry.approve(approver);
        
        journalPersistencePort.save(entry);
    }

    @Override
    @Transactional
    public void postJournalEntry(Long id, String poster) {
        JournalEntry entry = journalPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 전표입니다: " + id));
        
        // 도메인 엔티티에 위임 (Rich Domain Model)
        entry.post(poster);
        
        journalPersistencePort.save(entry);
    }
}
