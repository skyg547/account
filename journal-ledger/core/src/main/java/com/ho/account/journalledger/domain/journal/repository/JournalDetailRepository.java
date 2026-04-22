package com.ho.account.journal.repository;

import com.ho.account.journalledger.domain.journal.JournalDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JournalDetailRepository extends JpaRepository<JournalDetail, Long> {

       // ?뱀젙 怨꾩젙怨쇰ぉ??湲곌컙蹂??곸꽭 ?댁뿭 議고쉶 (?뱀씤???꾪몴留? ?뚭퀎?쇱옄 湲곗?)
       @Query("SELECT jd FROM JournalDetail jd " +
                     "JOIN jd.journalEntry je " +
                     "WHERE jd.accountSubject.code = :accountCode " +
                     "AND je.accountingDate BETWEEN :startDate AND :endDate " +
                     "AND je.status = 'APPROVED' " +
                     "ORDER BY je.accountingDate ASC, je.slipNo ASC")
       List<JournalDetail> findByAccountAndDateRange(
                     @Param("accountCode") String accountCode,
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate);

       // ?뱀젙 怨꾩젙怨쇰ぉ???꾧린 ?댁썡湲??쒖옉???댁쟾 ?붿븸) 怨꾩궛 (?뱀씤???꾪몴留? ?뚭퀎?쇱옄 湲곗?)
       @Query("SELECT jd FROM JournalDetail jd " +
                     "JOIN jd.journalEntry je " +
                     "WHERE jd.accountSubject.code = :accountCode " +
                     "AND je.accountingDate < :startDate " +
                     "AND je.status = 'APPROVED'")
       List<JournalDetail> findPreviousDetails(
                     @Param("accountCode") String accountCode,
                     @Param("startDate") LocalDate startDate);

       // ?먯씡怨꾩궛?쒖슜 湲곌컙蹂??꾩껜 ?댁뿭 議고쉶 (?뱀씤???꾪몴留?
       @Query("SELECT jd FROM JournalDetail jd " +
                     "JOIN jd.journalEntry je " +
                     "WHERE je.accountingDate BETWEEN :startDate AND :endDate " +
                     "AND je.status = 'APPROVED'")
       List<JournalDetail> findByAccountAndDateRangeForIS(
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate);

       // ?뱀젙 湲곌컙 ?댁뿉 ?꾧린(POSTED)???꾪몴??紐⑤뱺 ?곸꽭 ?댁뿭 議고쉶
       @Query("SELECT jd FROM JournalDetail jd " +
                     "JOIN jd.journalEntry je " +
                     "WHERE je.accountingDate BETWEEN :startDate AND :endDate " +
                     "AND je.status = 'POSTED'")
       List<JournalDetail> findPostedJournalDetailsByAccountingDateBetween(
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate);
}
