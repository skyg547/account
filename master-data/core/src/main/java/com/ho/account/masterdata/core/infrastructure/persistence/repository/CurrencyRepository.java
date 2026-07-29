package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import com.ho.account.masterdata.core.domain.model.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {
    @Query("""
            SELECT c FROM Currency c
            WHERE c.currencyCode = :currencyCode
              AND c.validFrom <= :date
              AND c.validTo >= :date
            ORDER BY c.validFrom DESC
            """)
    Optional<Currency> findActiveByCurrencyCode(String currencyCode, LocalDate date);

    default Optional<Currency> findByCurrencyCode(String currencyCode) {
        return findActiveByCurrencyCode(currencyCode, LocalDate.now());
    }
}
