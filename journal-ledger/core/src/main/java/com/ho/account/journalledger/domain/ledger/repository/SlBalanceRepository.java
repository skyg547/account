package com.ho.account.journalledger.domain.ledger.repository;

import com.ho.account.journalledger.domain.ledger.domain.SlBalance;
import org.springframework.data.jpa.repository.JpaRepository;
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
}