package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import com.ho.account.masterdata.core.infrastructure.persistence.entity.ExchangeRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRateEntity, Long> {
    Optional<ExchangeRateEntity>
            findFirstByFromCurrencyCodeAndToCurrencyCodeAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                    String fromCurrencyCode,
                    String toCurrencyCode,
                    LocalDate effectiveDate);

    default Optional<ExchangeRateEntity> findExchangeRate(
            String fromCurrencyCode,
            String toCurrencyCode,
            LocalDate date) {
        return findFirstByFromCurrencyCodeAndToCurrencyCodeAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                fromCurrencyCode,
                toCurrencyCode,
                date);
    }

    Optional<ExchangeRateEntity> findByFromCurrencyCodeAndToCurrencyCodeAndEffectiveDate(
            String fromCurrencyCode,
            String toCurrencyCode,
            LocalDate effectiveDate);
}

