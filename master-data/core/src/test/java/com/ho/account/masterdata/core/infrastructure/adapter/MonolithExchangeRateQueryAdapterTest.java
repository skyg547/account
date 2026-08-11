package com.ho.account.masterdata.core.infrastructure.adapter;

import com.ho.account.masterdata.core.infrastructure.persistence.entity.ExchangeRateEntity;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.ExchangeRateRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MonolithExchangeRateQueryAdapterTest {

    @Test
    void mapsProviderEntityToImmutableContractSnapshot() {
        ExchangeRateRepository repository = mock(ExchangeRateRepository.class);
        MonolithExchangeRateQueryAdapter adapter = new MonolithExchangeRateQueryAdapter(repository);
        LocalDate effectiveDate = LocalDate.of(2026, 5, 31);
        ExchangeRateEntity rate = new ExchangeRateEntity();
        rate.setFromCurrencyCode("EUR");
        rate.setToCurrencyCode("KRW");
        rate.setRate(new BigDecimal("1500.12345678"));
        rate.setEffectiveDate(effectiveDate);
        when(repository.findExchangeRate("EUR", "KRW", effectiveDate))
                .thenReturn(Optional.of(rate));

        var result = adapter.findLatestRateAt("EUR", "KRW", effectiveDate).orElseThrow();

        assertThat(result.fromCurrencyCode()).isEqualTo("EUR");
        assertThat(result.toCurrencyCode()).isEqualTo("KRW");
        assertThat(result.rate()).isEqualByComparingTo("1500.12345678");
        assertThat(result.effectiveDate()).isEqualTo(effectiveDate);
    }
}

