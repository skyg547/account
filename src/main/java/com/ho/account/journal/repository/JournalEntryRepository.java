package com.ho.account.journal.repository;

import com.ho.account.journal.domain.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // 누락된 import 추가
import org.springframework.data.repository.query.Param; // 누락된 import 추가
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

    /**
     * 특정 원천 시스템 유형과 ID에 해당하는 모든 분개 전표를 조회합니다.
     * IFRS 16 리스 관련 분개 추적에 사용될 수 있습니다.
     * @param lineageSourceType 원천 시스템 유형 (예: "IFRS16_LEASE")
     * @param lineageSourceId 원천 시스템 ID (예: 리스 계약 ID)
     * @return 해당하는 분개 전표 리스트
     */
    List<JournalEntry> findByLineageSourceTypeAndLineageSourceId(String lineageSourceType, String lineageSourceId);

    /**
     * 특정 회계일자와 원천 시스템 ID에 해당하는 모든 분개 전표를 조회합니다.
     * 월별 리스 회계 처리 분개 검증에 사용될 수 있습니다.
     * @param accountingDate 회계일자
     * @param lineageSourceId 원천 시스템 ID (예: 리스 계약 ID)
     * @return 해당하는 분개 전표 리스트
     */
    List<JournalEntry> findByAccountingDateAndLineageSourceId(LocalDate accountingDate, String lineageSourceId);

    @Query("SELECT je FROM JournalEntry je LEFT JOIN FETCH je.details WHERE je.id = :id")
    Optional<JournalEntry> findByIdWithDetails(@Param("id") Long id);
}
