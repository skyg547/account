package com.ho.account.basic.repository;

import com.ho.account.basic.domain.ExchangeRate;
import com.ho.account.basic.domain.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    Optional<ExchangeRate> findByBaseCurrencyAndTargetCurrencyAndApplyDate(
            Currency baseCurrency, Currency targetCurrency, LocalDate applyDate);

    // 가장 최근 환율 조회용 (Latest Rate)
    Optional<ExchangeRate> findFirstByBaseCurrencyAndTargetCurrencyAndApplyDateBeforeOrderByApplyDateDesc(
            Currency baseCurrency, Currency targetCurrency, LocalDate date);
}
