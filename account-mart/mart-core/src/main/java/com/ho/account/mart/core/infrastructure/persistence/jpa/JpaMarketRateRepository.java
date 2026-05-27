package com.ho.account.mart.core.infrastructure.persistence.jpa;

import com.ho.account.shared.finance.enums.CurrencyCode;
import com.ho.account.mart.core.infrastructure.persistence.entity.marketdata.MarketRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaMarketRateRepository extends JpaRepository<MarketRateEntity, Long> {

    List<MarketRateEntity> findByBaseDateAndIsActiveTrue(LocalDate baseDate);

    List<MarketRateEntity> findByBaseDateAndRateTypeAndIsActiveTrue(LocalDate baseDate, String rateType);

    List<MarketRateEntity> findByRateNameAndIsActiveTrue(String rateName);

    Optional<MarketRateEntity> findByBaseDateAndRateNameAndTenorMonths(LocalDate baseDate, String rateName, Integer tenorMonths);

    @Query("SELECT m FROM MarketRateEntity m WHERE m.rateName = :rateName AND m.tenorMonths = :tenorMonths " +
            "AND m.baseDate BETWEEN :startDate AND :endDate ORDER BY m.baseDate ASC")
    List<MarketRateEntity> findHistorical(
            @Param("rateName") String rateName,
            @Param("tenorMonths") Integer tenorMonths,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT DISTINCT m.rateName FROM MarketRateEntity m WHERE m.isActive = true ORDER BY m.rateName")
    List<String> findDistinctActiveRateNames();
}
