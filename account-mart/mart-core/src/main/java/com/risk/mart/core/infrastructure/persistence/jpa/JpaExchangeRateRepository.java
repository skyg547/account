package com.risk.mart.core.infrastructure.persistence.jpa;

import com.risk.common.enums.CurrencyCode;
import com.risk.mart.core.infrastructure.persistence.entity.marketdata.ExchangeRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaExchangeRateRepository extends JpaRepository<ExchangeRateEntity, Long> {
    Optional<ExchangeRateEntity> findByBaseDateAndBaseCurrencyAndQuoteCurrency(
            LocalDate baseDate, CurrencyCode baseCurrency, CurrencyCode quoteCurrency);
    
    List<ExchangeRateEntity> findByBaseDate(LocalDate baseDate);
    
    List<ExchangeRateEntity> findByBaseDateAndQuoteCurrency(LocalDate baseDate, CurrencyCode quoteCurrency);
    
    List<ExchangeRateEntity> findByBaseCurrencyAndQuoteCurrencyAndBaseDateBetweenOrderByBaseDateAsc(
            CurrencyCode baseCurrency, CurrencyCode quoteCurrency, LocalDate startDate, LocalDate endDate);

    @Query("SELECT e FROM ExchangeRateEntity e WHERE e.baseCurrency = :baseCurrency AND e.quoteCurrency = :quoteCurrency ORDER BY e.baseDate DESC LIMIT 1")
    Optional<ExchangeRateEntity> findLatest(@Param("baseCurrency") CurrencyCode baseCurrency, @Param("quoteCurrency") CurrencyCode quoteCurrency);
}
