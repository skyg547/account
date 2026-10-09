package com.ho.account.closing.infrastructure.external;

import com.ho.account.closing.application.port.out.FxExchangeRateLookupPort;
import com.ho.account.contracts.masterdata.ExchangeRateQueryPort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/** Batch-neutral bridge from the Closing FX port to the Master Data contract. */
public final class MasterDataFxExchangeRateLookupAdapter implements FxExchangeRateLookupPort {

    private final ExchangeRateQueryPort exchangeRateQueryPort;

    public MasterDataFxExchangeRateLookupAdapter(ExchangeRateQueryPort exchangeRateQueryPort) {
        this.exchangeRateQueryPort = Objects.requireNonNull(
                exchangeRateQueryPort, "exchangeRateQueryPort must not be null");
    }

    @Override
    public Optional<BigDecimal> findRate(
            String fromCurrencyCode,
            String toCurrencyCode,
            LocalDate effectiveDate) {
        return exchangeRateQueryPort.findLatestRateAt(fromCurrencyCode, toCurrencyCode, effectiveDate)
                .map(exchangeRate -> exchangeRate.rate());
    }
}
