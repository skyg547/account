package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import com.ho.account.masterdata.core.domain.model.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    @Query("""
            SELECT er FROM ExchangeRate er
            WHERE er.fromCurrencyCode = :fromCurrencyCode
              AND er.toCurrencyCode = :toCurrencyCode
              AND er.effectiveDate <= :date
            ORDER BY er.effectiveDate DESC
            """)
    Optional<ExchangeRate> findExchangeRate(String fromCurrencyCode, String toCurrencyCode, LocalDate date);

    Optional<ExchangeRate> findByFromCurrencyCodeAndToCurrencyCodeAndEffectiveDate(
            String fromCurrencyCode,
            String toCurrencyCode,
            LocalDate effectiveDate);
}
