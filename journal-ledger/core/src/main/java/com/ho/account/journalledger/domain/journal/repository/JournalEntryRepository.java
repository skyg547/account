package com.ho.account.journalledger.domain.journal.repository;

import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 전표 저장소 (Journal Entry Repository)
 */
@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    
    Optional<JournalEntry> findBySlipNo(String slipNo);
    
    List<JournalEntry> findBySlipDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<JournalEntry> findByAccountingDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<JournalEntry> findByAccountingDate(LocalDate accountingDate);

    /**
     * 원천 시스템 유형과 ID로 전표 목록을 조회합니다. (역추적용)
     */
    List<JournalEntry> findByLineageSourceTypeAndLineageSourceId(String lineageSourceType, String lineageSourceId);

    /**
     * 회계일자와 원천 ID로 전표 목록을 조회합니다.
     */
    List<JournalEntry> findByAccountingDateAndLineageSourceId(LocalDate accountingDate, String lineageSourceId);

    // PostgreSQL은 LEFT JOIN의 nullable 쪽에 FOR UPDATE를 적용할 수 없으므로 헤더만 잠급니다.
    @Query(value = "SELECT id FROM journal_entries WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Long> lockByIdForPosting(@Param("id") Long id);

    @Query("SELECT je FROM JournalEntry je LEFT JOIN FETCH je.details WHERE je.id = :id")
    Optional<JournalEntry> findByIdWithDetails(@Param("id") Long id);
}
