package com.ho.account.journalledger.domain.ledger.repository;

import com.ho.account.journalledger.domain.ledger.domain.SlBalance;
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
public interface SlBalanceRepository extends JpaRepository<SlBalance, Long> {

    Optional<SlBalance> findByAccountCodeAndBusinessPartnerCodeAndDepartmentCodeAndCurrencyCodeAndBalanceDateAndPeriod(
            String accountCode, String businessPartnerCode, String departmentCode,
            String currencyCode, LocalDate balanceDate, YearMonth period);

    List<SlBalance> findByBalanceDateBetween(LocalDate startDate, LocalDate endDate);

    @Modifying
    @Query("DELETE FROM SlBalance s WHERE s.balanceDate BETWEEN :startDate AND :endDate")
    int deleteByBalanceDateBetweenBulk(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<SlBalance> findByBalanceDateBetweenAndAccountCodeAndBusinessPartnerCodeAndDepartmentCodeAndCurrencyCode(
            LocalDate startDate, LocalDate endDate,
            String accountCode, String businessPartnerCode, String departmentCode,
            String currencyCode);

    Optional<SlBalance> findFirstByAccountCodeAndBusinessPartnerCodeAndDepartmentCodeAndCurrencyCodeAndBalanceDateBeforeOrderByBalanceDateDesc(
            String accountCode, String businessPartnerCode, String departmentCode,
            String currencyCode, LocalDate balanceDate);

    @Query("SELECT MAX(s.balanceDate) FROM SlBalance s")
    Optional<LocalDate> findLatestBalanceDate();

    @Modifying
    @Query("UPDATE SlBalance s " +
            "SET s.beginningBalance = s.beginningBalance + :delta, " +
            "s.endingBalance = s.endingBalance + :delta, " +
            "s.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE s.accountCode = :accountCode " +
            "AND ((:businessPartnerCode IS NULL AND s.businessPartnerCode IS NULL) " +
            "     OR s.businessPartnerCode = :businessPartnerCode) " +
            "AND ((:departmentCode IS NULL AND s.departmentCode IS NULL) " +
            "     OR s.departmentCode = :departmentCode) " +
            "AND s.currencyCode = :currencyCode " +
            "AND s.balanceDate > :postingDate")
    int shiftSuccessorBalances(
            @Param("accountCode") String accountCode,
            @Param("businessPartnerCode") String businessPartnerCode,
            @Param("departmentCode") String departmentCode,
            @Param("currencyCode") String currencyCode,
            @Param("postingDate") LocalDate postingDate,
            @Param("delta") BigDecimal delta);

    @Query("SELECT s FROM SlBalance s " +
            "WHERE s.balanceDate BETWEEN :startDate AND :endDate " +
            "AND (:accountCode IS NULL OR s.accountCode = :accountCode) " +
            "AND (:businessPartnerCode IS NULL OR s.businessPartnerCode = :businessPartnerCode) " +
            "AND (:departmentCode IS NULL OR s.departmentCode = :departmentCode) " +
            "AND (:currencyCode IS NULL OR s.currencyCode = :currencyCode)")
    List<SlBalance> findForQuery(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("accountCode") String accountCode,
            @Param("businessPartnerCode") String businessPartnerCode,
            @Param("departmentCode") String departmentCode,
            @Param("currencyCode") String currencyCode);
}
