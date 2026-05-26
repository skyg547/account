package com.risk.mart.core.application.port.out;

import com.risk.mart.core.domain.marketdata.MarketRate;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * [Outbound Port] 시장 금리 데이터 접근 인터페이스
 */
public interface MarketRateRepository {

    List<MarketRate> findByBaseDateAndIsActiveTrue(LocalDate baseDate);

    List<MarketRate> findByBaseDateAndRateTypeAndIsActiveTrue(LocalDate baseDate, String rateType);

    List<MarketRate> findByRateNameAndIsActiveTrue(String rateName);

    Optional<MarketRate> findByBaseDateAndRateNameAndTenorMonths(LocalDate baseDate, String rateName, Integer tenorMonths);

    List<MarketRate> findHistorical(String rateName, Integer tenorMonths, LocalDate startDate, LocalDate endDate);

    List<String> findDistinctActiveRateNames();

    MarketRate save(MarketRate rate);

    List<MarketRate> saveAll(List<MarketRate> rates);
}
