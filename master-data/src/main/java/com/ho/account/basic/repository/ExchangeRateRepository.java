package com.ho.account.basic.repository;

import com.ho.account.basic.domain.Currency;
import com.ho.account.basic.domain.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    @Query("SELECT er FROM ExchangeRate er WHERE er.fromCurrency = :fromCurrency AND er.toCurrency = :toCurrency AND er.effectiveDate <= :date ORDER BY er.effectiveDate DESC")
    Optional<ExchangeRate> findExchangeRate(Currency fromCurrency, Currency toCurrency, LocalDate date);

    Optional<ExchangeRate> findByFromCurrencyAndToCurrencyAndEffectiveDate(Currency fromCurrency, Currency toCurrency, LocalDate effectiveDate);
}
