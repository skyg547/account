package com.ho.account.journalledger.domain.ledger.repository;

import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * GL 잔액 레포지토리 (GlBalance Repository)
 */
@Repository
public interface GlBalanceRepository extends JpaRepository<GlBalance, Long> {

    Optional<GlBalance> findByAccountSubjectAndCurrencyAndBalanceDateAndPeriod(
            AccountSubject accountSubject, Currency currency, LocalDate balanceDate, YearMonth period);

    List<GlBalance> findByBalanceDateBetween(LocalDate startDate, LocalDate endDate);

    List<GlBalance> findByBalanceDateBetweenAndAccountSubjectAndCurrency(
            LocalDate startDate, LocalDate endDate,
            AccountSubject accountSubject, Currency currency);

    // 잔액 이월용: 특정 일자 이전의 마지막 잔액 조회
    Optional<GlBalance> findFirstByAccountSubjectAndCurrencyAndBalanceDateBeforeOrderByBalanceDateDesc(
            AccountSubject accountSubject, Currency currency, LocalDate balanceDate);
}
