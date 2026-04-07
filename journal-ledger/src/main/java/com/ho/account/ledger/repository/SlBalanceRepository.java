package com.ho.account.ledger.repository;

import com.ho.account.ledger.domain.SlBalance;
import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List; // 누락된 import 추가
import java.util.Optional;

/**
 * SlBalance 엔티티를 위한 Spring Data JPA 리포지토리
 * 특정 계정과목, 거래처, 부서, 날짜, 기간에 대한 보조원장 잔액 데이터를 관리합니다.
 */
@Repository
public interface SlBalanceRepository extends JpaRepository<SlBalance, Long> {

    /**
     * 특정 계정과목, 거래처, 부서, 일자, 기간에 해당하는 SlBalance를 조회합니다.
     * @param accountSubject 계정과목
     * @param businessPartner 거래처 (null 허용)
     * @param department 부서 (null 허용)
     * @param balanceDate 잔액 일자
     * @param period 회계 기간 (년월)
     * @return 해당 조건을 만족하는 SlBalance 엔티티 (존재하지 않을 경우 Optional.empty())
     */
    Optional<SlBalance> findByAccountSubjectAndBusinessPartnerAndDepartmentAndCurrencyAndBalanceDateAndPeriod(
            AccountSubject accountSubject, BusinessPartner businessPartner, Department department,
            com.ho.account.basic.domain.Currency currency, LocalDate balanceDate, YearMonth period);

    // TODO: Add methods for querying by date ranges, or other combinations as needed for reporting
    List<SlBalance> findByBalanceDateBetween(LocalDate startDate, LocalDate endDate);

    List<SlBalance> findByBalanceDateBetweenAndAccountSubjectAndBusinessPartnerAndDepartmentAndCurrency(
            LocalDate startDate, LocalDate endDate,
            AccountSubject accountSubject, BusinessPartner businessPartner, Department department,
            com.ho.account.basic.domain.Currency currency
    );
}
