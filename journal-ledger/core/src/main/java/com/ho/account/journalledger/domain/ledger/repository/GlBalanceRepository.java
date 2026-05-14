package com.ho.account.journalledger.domain.ledger.repository;

import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public interface GlBalanceRepository extends JpaRepository<GlBalance, Long> {

    Optional<GlBalance> findByAccountCodeAndCurrencyCodeAndBalanceDateAndPeriod(
            String accountCode, String currencyCode, LocalDate balanceDate, YearMonth period);

    List<GlBalance> findByBalanceDateBetween(LocalDate startDate, LocalDate endDate);

    List<GlBalance> findByBalanceDateBetweenAndAccountCodeAndCurrencyCode(
            LocalDate startDate, LocalDate endDate,
            String accountCode, String currencyCode);

    Optional<GlBalance> findFirstByAccountCodeAndCurrencyCodeAndBalanceDateBeforeOrderByBalanceDateDesc(
            String accountCode, String currencyCode, LocalDate balanceDate);
}