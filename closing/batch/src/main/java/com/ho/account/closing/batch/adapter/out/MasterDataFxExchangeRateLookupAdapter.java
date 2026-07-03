package com.ho.account.closing.batch.adapter.out;

import com.ho.account.closing.application.port.out.FxExchangeRateLookupPort;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MasterDataFxExchangeRateLookupAdapter implements FxExchangeRateLookupPort {

    private final ExchangeRateRepository exchangeRateRepository;

    @Override
    public Optional<BigDecimal> findRate(String fromCurrencyCode, String toCurrencyCode, LocalDate effectiveDate) {
        return exchangeRateRepository.findExchangeRate(fromCurrencyCode, toCurrencyCode, effectiveDate)
                .map(exchangeRate -> exchangeRate.getRate());
    }
}