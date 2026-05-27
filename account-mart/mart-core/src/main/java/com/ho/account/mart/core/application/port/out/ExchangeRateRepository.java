package com.ho.account.mart.core.application.port.out;

import com.ho.account.shared.finance.enums.CurrencyCode;
import com.ho.account.mart.core.domain.marketdata.ExchangeRate;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * [Outbound Port] 환율 데이터 접근 인터페이스
 */
public interface ExchangeRateRepository {

    Optional<ExchangeRate> findByBaseDateAndBaseCurrencyAndQuoteCurrency(
            LocalDate baseDate, CurrencyCode baseCurrency, CurrencyCode quoteCurrency);

    List<ExchangeRate> findByBaseDate(LocalDate baseDate);

    List<ExchangeRate> findByBaseDateAndQuoteCurrency(LocalDate baseDate, CurrencyCode quoteCurrency);

    List<ExchangeRate> findHistorical(CurrencyCode baseCurrency, CurrencyCode quoteCurrency, LocalDate startDate, LocalDate endDate);

    Optional<ExchangeRate> findLatest(CurrencyCode baseCurrency, CurrencyCode quoteCurrency);

    ExchangeRate save(ExchangeRate rate);

    void saveAll(List<ExchangeRate> rates);

    List<ExchangeRate> findAll();
}
