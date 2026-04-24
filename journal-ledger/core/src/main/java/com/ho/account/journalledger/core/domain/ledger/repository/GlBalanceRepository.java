package com.ho.account.journalledger.core.domain.ledger.repository;

import com.ho.account.journalledger.core.domain.ledger.domain.GlBalance;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * GlBalance 엔티티를 위한 Spring Data JPA 리포지토리
 * 특정 계정과목의 잔액, 기간별 원장 잔액 데이터를 관리한다.
 */
@Repository
public interface GlBalanceRepository extends JpaRepository<GlBalance, Long> {

    /**
     * 특정 계정과목, 통화, 일자, 기간에 해당하는 GlBalance를 조회합니다.
     * @param accountSubject 계정과목
     * @param currency 통화
     * @param balanceDate 잔액 일자
     * @param period 정산기간(년월)
     * @return 해당 조건에 맞는 GlBalance 엔티티(존재하지 않을 경우 Optional.empty())
     */
    Optional<GlBalance> findByAccountSubjectAndCurrencyAndBalanceDateAndPeriod(AccountSubject accountSubject, com.ho.account.masterdata.core.domain.model.Currency currency, LocalDate balanceDate, YearMonth period);

    /**
     * 일자 범위 내의 GlBalance 리스트를 조회합니다.
     * @param startDate 시작 일자
     * @param endDate 종료 일자
     * @return 해당 조건에 맞는 GlBalance 리스트
     */
    List<GlBalance> findByBalanceDateBetween(LocalDate startDate, LocalDate endDate);

    List<GlBalance> findByBalanceDateBetweenAndAccountSubjectAndCurrency(
            LocalDate startDate, LocalDate endDate,
            AccountSubject accountSubject, com.ho.account.masterdata.core.domain.model.Currency currency
    );
}
