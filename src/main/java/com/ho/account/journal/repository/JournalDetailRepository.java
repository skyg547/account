package com.accounting.system.journal.repository;

import com.accounting.system.journal.domain.JournalDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JournalDetailRepository extends JpaRepository<JournalDetail, Long> {

    // 특정 계정과목의 기간별 상세 내역 조회 (승인된 전표만, 회계일자 기준)
    @Query("SELECT jd FROM JournalDetail jd " +
           "JOIN jd.journalEntry je " +
           "WHERE jd.accountSubject.accountCode = :accountCode " +
           "AND je.accountingDate BETWEEN :startDate AND :endDate " +
           "AND je.status = 'APPROVED' " +
           "ORDER BY je.accountingDate ASC, je.slipNo ASC")
    List<JournalDetail> findByAccountAndDateRange(
            @Param("accountCode") String accountCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 특정 계정과목의 전기 이월금(시작일 이전 잔액) 계산 (승인된 전표만, 회계일자 기준)
    @Query("SELECT jd FROM JournalDetail jd " +
           "JOIN jd.journalEntry je " +
           "WHERE jd.accountSubject.accountCode = :accountCode " +
           "AND je.accountingDate < :startDate " +
           "AND je.status = 'APPROVED'")
    List<JournalDetail> findPreviousDetails(
            @Param("accountCode") String accountCode,
            @Param("startDate") LocalDate startDate);

    // 손익계산서용 기간별 전체 내역 조회 (승인된 전표만)
    @Query("SELECT jd FROM JournalDetail jd " +
           "JOIN jd.journalEntry je " +
           "WHERE je.accountingDate BETWEEN :startDate AND :endDate " +
           "AND je.status = 'APPROVED'")
    List<JournalDetail> findByAccountAndDateRangeForIS(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
