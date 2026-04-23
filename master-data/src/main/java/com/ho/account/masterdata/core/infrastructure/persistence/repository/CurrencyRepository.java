package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import com.ho.account.masterdata.core.domain.model.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, String> {
    Optional<Currency> findByCurrencyCode(String currencyCode);
}
