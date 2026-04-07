package com.ho.account.ledger.repository;

import com.ho.account.ledger.domain.GlBalance;
import com.ho.account.basic.domain.AccountSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List; // 누락된 import 추가
import java.util.Optional;

/**
 * GlBalance 엔티티를 위한 Spring Data JPA 리포지토리
 * 특정 계정과목과 날짜, 기간에 대한 총계정원장 잔액 데이터를 관리합니다.
 */
@Repository
public interface GlBalanceRepository extends JpaRepository<GlBalance, Long> {

    /**
     * 특정 계정과목, 일자, 기간에 해당하는 GlBalance를 조회합니다.
     * @param accountSubject 계정과목
     * @param balanceDate 잔액 일자
     * @param period 회계 기간 (년월)
     * @return 해당 조건을 만족하는 GlBalance 엔티티 (존재하지 않을 경우 Optional.empty())
     */
    Optional<GlBalance> findByAccountSubjectAndCurrencyAndBalanceDateAndPeriod(AccountSubject accountSubject, com.ho.account.basic.domain.Currency currency, LocalDate balanceDate, YearMonth period);

    /**
     * 특정 계정과목과 일자 범위에 해당하는 GlBalance 리스트를 조회합니다.
     * @param accountSubject 계정과목
     * @param startDate 시작 일자
     * @param endDate 종료 일자
     * @return 해당 조건을 만족하는 GlBalance 리스트
     */
    // List<GlBalance> findByAccountSubjectAndBalanceDateBetween(AccountSubject accountSubject, LocalDate startDate, LocalDate endDate);
    List<GlBalance> findByBalanceDateBetween(LocalDate startDate, LocalDate endDate);

    List<GlBalance> findByBalanceDateBetweenAndAccountSubjectAndCurrency(
            LocalDate startDate, LocalDate endDate,
            AccountSubject accountSubject, com.ho.account.basic.domain.Currency currency
    );
}
