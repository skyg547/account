package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import com.ho.account.masterdata.core.domain.model.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    Optional<ExchangeRate>
            findFirstByFromCurrencyCodeAndToCurrencyCodeAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                    String fromCurrencyCode,
                    String toCurrencyCode,
                    LocalDate effectiveDate);

    default Optional<ExchangeRate> findExchangeRate(
            String fromCurrencyCode,
            String toCurrencyCode,
            LocalDate date) {
        return findFirstByFromCurrencyCodeAndToCurrencyCodeAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                fromCurrencyCode,
                toCurrencyCode,
                date);
    }

    Optional<ExchangeRate> findByFromCurrencyCodeAndToCurrencyCodeAndEffectiveDate(
            String fromCurrencyCode,
            String toCurrencyCode,
            LocalDate effectiveDate);
}
