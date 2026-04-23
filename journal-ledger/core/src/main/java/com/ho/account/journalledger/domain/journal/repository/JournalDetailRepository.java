package com.ho.account.journal.repository;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JournalDetailRepository extends JpaRepository<JournalDetail, Long> {

       // ?뱀???�꾩?�怨쇰???湲곌컙蹂??곸꽭 ??�뿭 議고??(?뱀????꾪몴�? ???�??�옄 湲곗?)
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

       // ?뱀???�꾩?�怨쇰????꾧린 ??�썡�???�옉????�쟾 ?붿븸) ?�꾩�?(?뱀????꾪몴�? ???�??�옄 湲곗?)
       @Query("SELECT jd FROM JournalDetail jd " +
                     "JOIN jd.journalEntry je " +
                     "WHERE jd.accountSubject.code = :accountCode " +
                     "AND je.accountingDate < :startDate " +
                     "AND je.status = 'APPROVED'")
       List<JournalDetail> findPreviousDetails(
                     @Param("accountCode") String accountCode,
                     @Param("startDate") LocalDate startDate);

       // ?�?��?�꾩�??�슜 湲곌컙蹂??꾩껜 ??�뿭 議고??(?뱀????꾪몴�?
       @Query("SELECT jd FROM JournalDetail jd " +
                     "JOIN jd.journalEntry je " +
                     "WHERE je.accountingDate BETWEEN :startDate AND :endDate " +
                     "AND je.status = 'APPROVED'")
       List<JournalDetail> findByAccountAndDateRangeForIS(
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate);

       // ?뱀??湲곌�???�뿉 ?꾧린(POSTED)???꾪몴??紐⑤�??곸꽭 ??�뿭 議고??
       @Query("SELECT jd FROM JournalDetail jd " +
                     "JOIN jd.journalEntry je " +
                     "WHERE je.accountingDate BETWEEN :startDate AND :endDate " +
                     "AND je.status = 'POSTED'")
       List<JournalDetail> findPostedJournalDetailsByAccountingDateBetween(
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate);
}
