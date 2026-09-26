package com.ho.account.journalledger.domain.ledger.repository;

import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public interface GlBalanceRepository extends JpaRepository<GlBalance, Long> {

    @Query(value = "SELECT lock_id FROM ledger_balance_locks "
            + "WHERE lock_id IN (:lockIds) ORDER BY lock_id FOR UPDATE", nativeQuery = true)
    List<Integer> lockBalanceStripes(@Param("lockIds") List<Integer> lockIds);

    Optional<GlBalance> findByAccountCodeAndCurrencyCodeAndBalanceDateAndPeriod(
            String accountCode, String currencyCode, LocalDate balanceDate, YearMonth period);

    List<GlBalance> findByBalanceDateBetween(LocalDate startDate, LocalDate endDate);

    @Modifying
    @Query("DELETE FROM GlBalance g WHERE g.balanceDate BETWEEN :startDate AND :endDate")
    int deleteByBalanceDateBetweenBulk(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<GlBalance> findByBalanceDateBetweenAndAccountCodeAndCurrencyCode(
            LocalDate startDate, LocalDate endDate,
            String accountCode, String currencyCode);

    Optional<GlBalance> findFirstByAccountCodeAndCurrencyCodeAndBalanceDateBeforeOrderByBalanceDateDesc(
            String accountCode, String currencyCode, LocalDate balanceDate);

    @Query("SELECT MAX(g.balanceDate) FROM GlBalance g")
    Optional<LocalDate> findLatestBalanceDate();

    @Modifying
    @Query("UPDATE GlBalance g " +
            "SET g.beginningBalance = g.beginningBalance + :delta, " +
            "g.endingBalance = g.endingBalance + :delta, " +
            "g.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE g.accountCode = :accountCode " +
            "AND g.currencyCode = :currencyCode " +
            "AND g.balanceDate > :postingDate")
    int shiftSuccessorBalances(
            @Param("accountCode") String accountCode,
            @Param("currencyCode") String currencyCode,
            @Param("postingDate") LocalDate postingDate,
            @Param("delta") BigDecimal delta);

    @Query("SELECT g FROM GlBalance g " +
            "WHERE g.balanceDate BETWEEN :startDate AND :endDate " +
            "AND (:accountCode IS NULL OR g.accountCode = :accountCode) " +
            "AND (:currencyCode IS NULL OR g.currencyCode = :currencyCode)")
    List<GlBalance> findForQuery(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("accountCode") String accountCode,
            @Param("currencyCode") String currencyCode);

    @Query("SELECT COUNT(g), " +
            "SUM(CASE WHEN :amountBasis = 'CREDIT' THEN g.creditAmount " +
            "         WHEN :amountBasis = 'ENDING_BALANCE' THEN g.endingBalance " +
            "         WHEN :amountBasis = 'ABS_ENDING_BALANCE' THEN ABS(g.endingBalance) " +
            "         ELSE g.debitAmount END) " +
            "FROM GlBalance g " +
            "WHERE g.balanceDate BETWEEN :startDate AND :endDate " +
            "AND (:accountCode IS NULL OR g.accountCode = :accountCode) " +
            "AND (:currencyCode IS NULL OR g.currencyCode = :currencyCode)")
    Object calculateGlBalanceAggregate(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("accountCode") String accountCode,
            @Param("currencyCode") String currencyCode,
            @Param("amountBasis") String amountBasis);
}
