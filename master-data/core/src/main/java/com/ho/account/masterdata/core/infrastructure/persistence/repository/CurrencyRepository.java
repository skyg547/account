package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import com.ho.account.masterdata.core.infrastructure.persistence.entity.CurrencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface CurrencyRepository extends JpaRepository<CurrencyEntity, Long> {
    @Query("""
            SELECT c FROM CurrencyEntity c
            WHERE c.currencyCode = :currencyCode
              AND c.validFrom <= :date
              AND c.validTo >= :date
            ORDER BY c.validFrom DESC
            """)
    Optional<CurrencyEntity> findActiveByCurrencyCode(String currencyCode, LocalDate date);

    default Optional<CurrencyEntity> findByCurrencyCode(String currencyCode) {
        return findActiveByCurrencyCode(currencyCode, LocalDate.now());
    }
}

