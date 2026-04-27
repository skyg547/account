package com.ho.account.journalledger.domain.ledger.repository;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.Currency;
import com.ho.account.journalledger.domain.ledger.domain.GlAccountBalance;
import com.ho.account.journalledger.domain.ledger.domain.GlBalanceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GlAccountBalanceRepository extends JpaRepository<GlAccountBalance, Long> {
    Optional<GlAccountBalance> findByAccountSubjectAndCurrencyAndBalanceDateAndBalanceType(
            AccountSubject accountSubject, Currency currency, LocalDate balanceDate, GlBalanceType balanceType);

    List<GlAccountBalance> findByAccountSubjectAndBalanceDateBetween(
            AccountSubject accountSubject, LocalDate startDate, LocalDate endDate);

    List<GlAccountBalance> findByBalanceDateBetween(LocalDate startDate, LocalDate endDate);
}
