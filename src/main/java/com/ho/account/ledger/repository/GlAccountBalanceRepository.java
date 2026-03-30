package com.ho.account.ledger.repository;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.Currency;
import com.ho.account.ledger.domain.GlAccountBalance;
import com.ho.account.ledger.domain.GlBalanceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GlAccountBalanceRepository extends JpaRepository<GlAccountBalance, Long> {
    Optional<GlAccountBalance> findByAccountSubjectAndCurrencyAndAccountingDateAndBalanceType(
            AccountSubject accountSubject, Currency currency, LocalDate accountingDate, GlBalanceType balanceType);

    List<GlAccountBalance> findByAccountSubjectAndAccountingDateBetween(
            AccountSubject accountSubject, LocalDate startDate, LocalDate endDate);

    List<GlAccountBalance> findByAccountingDateBetween(LocalDate startDate, LocalDate endDate);
}
