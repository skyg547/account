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
import java.util.Optional;

/**
 * 전표 상세 저장소 (Journal Detail Repository)
 * 각 전표의 개별 분개 항목(차변/대변)을 관리합니다.
 */
@Repository
public interface JournalDetailRepository extends JpaRepository<JournalDetail, Long> {

       /*
        * SimpleJpaRepository의 bulk delete는 엔티티를 로드하지 않아 @PreRemove를 실행하지 않습니다.
        * 일반 delete API만 남겨 DRAFT 삭제와 최종 이력 차단을 엔티티 callback이 일관되게 판정하게 합니다.
        */
       @Override
       default void deleteAllInBatch() {
              throw bulkDeleteDisabled();
       }

       @Override
       default void deleteAllInBatch(Iterable<JournalDetail> entities) {
              throw bulkDeleteDisabled();
       }

       @Override
       default void deleteAllByIdInBatch(Iterable<Long> ids) {
              throw bulkDeleteDisabled();
       }

       @Override
       @Deprecated
       default void deleteInBatch(Iterable<JournalDetail> entities) {
              throw bulkDeleteDisabled();
       }

       private static IllegalStateException bulkDeleteDisabled() {
              return new IllegalStateException(
                            "POSTED 전표 이력 보호를 우회하는 JPA bulk delete는 사용할 수 없습니다.");
       }

       // 재무 조회에는 원장 반영이 완료된 POSTED 전표만 포함합니다.
       // APPROVED는 결재가 끝났지만 아직 원장에 반영되지 않은 상태이므로 잔액/보고 수치에서 제외합니다.
       /**
        * 특정 계정과목의 기간별 상세 내역 조회 (회계일자 기준)
        */
       @Query("SELECT jd FROM JournalDetail jd " +
                     "JOIN jd.journalEntry je " +
                     "WHERE jd.accountCode = :accountCode " +
                     "AND je.accountingDate BETWEEN :startDate AND :endDate " +
                     "AND je.status = 'POSTED' " +
                     "ORDER BY je.accountingDate ASC, je.slipNo ASC")
       List<JournalDetail> findByAccountAndDateRange(
                     @Param("accountCode") String accountCode,
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate);

       /**
        * 다수의 계정과목에 대해 기간별 상세 내역 조회 (회계일자 기준)
        */
       @Query("SELECT jd FROM JournalDetail jd " +
                     "JOIN jd.journalEntry je " +
                     "WHERE jd.accountCode IN :accountCodes " +
                     "AND je.accountingDate BETWEEN :startDate AND :endDate " +
                     "AND je.status = 'POSTED' " +
                     "ORDER BY je.accountingDate ASC, je.slipNo ASC")
       List<JournalDetail> findByAccountCodesAndDateRange(
                     @Param("accountCodes") List<String> accountCodes,
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate);

       /**
        * 특정 계정과목의 전기 이월분 조회 (시작일 이전 합계 산출용)
        */
       @Query("SELECT jd FROM JournalDetail jd " +
                     "JOIN jd.journalEntry je " +
                     "WHERE jd.accountCode = :accountCode " +
                     "AND je.accountingDate < :startDate " +
                     "AND je.status = 'POSTED'")
       List<JournalDetail> findPreviousDetails(
                     @Param("accountCode") String accountCode,
                     @Param("startDate") LocalDate startDate);

       /**
        * 시산표용 기간별 전체 상세 내역 조회
        */
       @Query("SELECT jd FROM JournalDetail jd " +
                     "JOIN jd.journalEntry je " +
                     "WHERE je.accountingDate BETWEEN :startDate AND :endDate " +
                     "AND je.status = 'POSTED'")
       List<JournalDetail> findByAccountAndDateRangeForIS(
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate);

       /**
        * 특정 기간 내에 전기(POSTED)된 모든 전표 상세 내역 조회
        * 날짜와 PK 순서를 고정해 직접 재집계가 Batch reader와 같은 순서로 원천 행을 받습니다.
        */
       @Query("SELECT jd FROM JournalDetail jd " +
                     "JOIN jd.journalEntry je " +
                     "WHERE je.accountingDate BETWEEN :startDate AND :endDate " +
                     "AND je.status = 'POSTED' " +
                     "ORDER BY je.accountingDate ASC, je.id ASC, jd.id ASC")
       List<JournalDetail> findPostedJournalDetailsByAccountingDateBetween(
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate);

       @Query("SELECT MAX(je.accountingDate) FROM JournalEntry je WHERE je.status = 'POSTED'")
       Optional<LocalDate> findLatestPostedAccountingDate();

       /**
        * 기간과 차대변 방향, 그리고 계정코드 기준의 전기(POSTED) 완료된 전표 상세 금액을 DB에서 직접 집계합니다.
        */
       @Query("SELECT COUNT(jd) AS detailCount, " +
                     "COALESCE(SUM(COALESCE(jd.baseAmount, jd.amount)), 0) AS totalAmount " +
                     "FROM JournalDetail jd " +
                     "JOIN jd.journalEntry je " +
                     "WHERE je.accountingDate BETWEEN :startDate AND :endDate " +
                     "AND jd.side = :side " +
                     "AND je.status = 'POSTED' " +
                     "AND (:accountCode IS NULL OR jd.accountCode = :accountCode)")
       JournalDetailAggregateProjection summarizeByAccountingDateBetweenAndSideAndAccountCode(
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate,
                     @Param("side") JournalSide side,
                     @Param("accountCode") String accountCode);

       /**
        * 기간과 차대변 방향 기준의 전표 상세 금액을 DB에서 직접 집계합니다. (하위 호환성 유지)
        */
       @Query("SELECT COUNT(jd) AS detailCount, " +
                     "COALESCE(SUM(COALESCE(jd.baseAmount, jd.amount)), 0) AS totalAmount " +
                     "FROM JournalDetail jd " +
                     "JOIN jd.journalEntry je " +
                     "WHERE je.accountingDate BETWEEN :startDate AND :endDate " +
                     "AND jd.side = :side " +
                     "AND je.status = 'POSTED'")
       JournalDetailAggregateProjection summarizeByAccountingDateBetweenAndSide(
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate,
                     @Param("side") JournalSide side);

       interface JournalDetailAggregateProjection {
              Long getDetailCount();

              BigDecimal getTotalAmount();
       }
}
