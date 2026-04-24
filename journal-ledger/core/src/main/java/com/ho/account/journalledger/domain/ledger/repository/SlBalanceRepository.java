package com.ho.account.journalledger.domain.ledger.repository;

import com.ho.account.journalledger.domain.ledger.domain.SlBalance;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Currency;
import com.ho.account.masterdata.core.domain.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * SL 잔액 레포지토리 (SlBalance Repository)
 */
@Repository
public interface SlBalanceRepository extends JpaRepository<SlBalance, Long> {

    Optional<SlBalance> findByAccountSubjectAndBusinessPartnerAndDepartmentAndCurrencyAndBalanceDateAndPeriod(
            AccountSubject accountSubject, BusinessPartner businessPartner, Department department,
            Currency currency, LocalDate balanceDate, YearMonth period);

    List<SlBalance> findByBalanceDateBetween(LocalDate startDate, LocalDate endDate);

    List<SlBalance> findByBalanceDateBetweenAndAccountSubjectAndBusinessPartnerAndDepartmentAndCurrency(
            LocalDate startDate, LocalDate endDate,
            AccountSubject accountSubject, BusinessPartner businessPartner, Department department,
            Currency currency);

    // 잔액 이월용: 특정 일자 이전의 마지막 잔액 조회
    Optional<SlBalance> findFirstByAccountSubjectAndBusinessPartnerAndDepartmentAndCurrencyAndBalanceDateBeforeOrderByBalanceDateDesc(
            AccountSubject accountSubject, BusinessPartner businessPartner, Department department,
            Currency currency, LocalDate balanceDate);
}
