package com.ho.account.journalledger.domain.journal.repository;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 전표 상세 저장소 (Journal Detail Repository)
 * 각 전표의 개별 분개 항목(차변/대변)을 관리합니다.
 */
@Repository
public interface JournalDetailRepository extends JpaRepository<JournalDetail, Long> {

       /**
        * 특정 계정과목의 기간별 상세 내역 조회 (회계일자 기준)
        */
       @Query("SELECT jd FROM JournalDetail jd " +
                     "JOIN jd.journalEntry je " +
                     "WHERE jd.accountCode = :accountCode " +
                     "AND je.accountingDate BETWEEN :startDate AND :endDate " +
                     "AND je.status = 'APPROVED' " +
                     "ORDER BY je.accountingDate ASC, je.slipNo ASC")
       List<JournalDetail> findByAccountAndDateRange(
                     @Param("accountCode") String accountCode,
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate);

       /**
        * 특정 계정과목의 전기 이월분 조회 (시작일 이전 합계 산출용)
        */
       @Query("SELECT jd FROM JournalDetail jd " +
                     "JOIN jd.journalEntry je " +
                     "WHERE jd.accountCode = :accountCode " +
                     "AND je.accountingDate < :startDate " +
                     "AND je.status = 'APPROVED'")
       List<JournalDetail> findPreviousDetails(
                     @Param("accountCode") String accountCode,
                     @Param("startDate") LocalDate startDate);

       /**
        * 시산표용 기간별 전체 상세 내역 조회
        */
       @Query("SELECT jd FROM JournalDetail jd " +
                     "JOIN jd.journalEntry je " +
                     "WHERE je.accountingDate BETWEEN :startDate AND :endDate " +
                     "AND je.status = 'APPROVED'")
       List<JournalDetail> findByAccountAndDateRangeForIS(
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate);

       /**
        * 특정 기간 내에 전기(POSTED)된 모든 전표 상세 내역 조회
        */
       @Query("SELECT jd FROM JournalDetail jd " +
                     "JOIN jd.journalEntry je " +
                     "WHERE je.accountingDate BETWEEN :startDate AND :endDate " +
                     "AND je.status = 'POSTED'")
       List<JournalDetail> findPostedJournalDetailsByAccountingDateBetween(
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate);

       /**
        * 기간과 차대변 방향 기준의 전표 상세 금액을 DB에서 직접 집계합니다.
        */
       @Query("SELECT COUNT(jd) AS detailCount, " +
                     "COALESCE(SUM(COALESCE(jd.baseAmount, jd.amount)), 0) AS totalAmount " +
                     "FROM JournalDetail jd " +
                     "JOIN jd.journalEntry je " +
                     "WHERE je.accountingDate BETWEEN :startDate AND :endDate " +
                     "AND jd.side = :side")
       JournalDetailAggregateProjection summarizeByAccountingDateBetweenAndSide(
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate,
                     @Param("side") JournalSide side);

       interface JournalDetailAggregateProjection {
              Long getDetailCount();

              BigDecimal getTotalAmount();
       }
}
