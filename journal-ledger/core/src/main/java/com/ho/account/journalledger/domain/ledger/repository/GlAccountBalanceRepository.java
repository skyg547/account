package com.ho.account.journalledger.domain.ledger.repository;

import com.ho.account.journalledger.domain.ledger.domain.GlAccountBalance;
import com.ho.account.journalledger.domain.ledger.domain.GlBalanceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GlAccountBalanceRepository extends JpaRepository<GlAccountBalance, Long> {

    Optional<GlAccountBalance> findByAccountCodeAndCurrencyCodeAndBalanceDateAndBalanceType(
            String accountCode, String currencyCode, LocalDate balanceDate, GlBalanceType balanceType);

    List<GlAccountBalance> findByAccountCodeAndBalanceDateBetween(
            String accountCode, LocalDate startDate, LocalDate endDate);
}