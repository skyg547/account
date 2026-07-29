package com.ho.account.masterdata.core.infrastructure.adapter;

import com.ho.account.contracts.masterdata.ExchangeRateQueryPort;
import com.ho.account.contracts.masterdata.ExchangeRateRef;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

/** Local modular-monolith adapter. Remote deployments can replace it with an API adapter. */
@Component
@RequiredArgsConstructor
public class MonolithExchangeRateQueryAdapter implements ExchangeRateQueryPort {

    private final ExchangeRateRepository exchangeRateRepository;

    @Override
    public Optional<ExchangeRateRef> findLatestRateAt(
            String fromCurrencyCode,
            String toCurrencyCode,
            LocalDate effectiveDate) {
        return exchangeRateRepository.findExchangeRate(fromCurrencyCode, toCurrencyCode, effectiveDate)
                .map(rate -> new ExchangeRateRef(
                        rate.getFromCurrencyCode(),
                        rate.getToCurrencyCode(),
                        rate.getRate(),
                        rate.getEffectiveDate()));
    }
}
