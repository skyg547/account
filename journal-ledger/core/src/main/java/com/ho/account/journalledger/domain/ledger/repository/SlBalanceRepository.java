package com.ho.account.journalledger.domain.ledger.repository;

import com.ho.account.journalledger.domain.ledger.domain.SlBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    List<SlBalance> findByBalanceDateBetweenAndAccountCodeAndBusinessPartnerCodeAndDepartmentCodeAndCurrencyCode(
            LocalDate startDate, LocalDate endDate,
            String accountCode, String businessPartnerCode, String departmentCode,
            String currencyCode);

    Optional<SlBalance> findFirstByAccountCodeAndBusinessPartnerCodeAndDepartmentCodeAndCurrencyCodeAndBalanceDateBeforeOrderByBalanceDateDesc(
            String accountCode, String businessPartnerCode, String departmentCode,
            String currencyCode, LocalDate balanceDate);

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
