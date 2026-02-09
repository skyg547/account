package com.ho.account.journal.repository;

import com.ho.account.journal.domain.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    Optional<JournalEntry> findBySlipNo(String slipNo);
    List<JournalEntry> findBySlipDateBetween(LocalDate startDate, LocalDate endDate);
    
    // 회계일자 기준 조회
    List<JournalEntry> findByAccountingDateBetween(LocalDate startDate, LocalDate endDate);
    
    // 특정 회계일자의 전표 조회 (채번용)
    List<JournalEntry> findByAccountingDate(LocalDate accountingDate);
}
