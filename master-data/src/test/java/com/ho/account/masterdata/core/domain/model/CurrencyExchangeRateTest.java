package com.ho.account.masterdata.core.domain.model;

import jakarta.persistence.ManyToOne;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyExchangeRateTest {

    @Test
    void currencyTerminateClosesValidityWindow() {
        Currency currency = new Currency();
        currency.setCurrencyCode("USD");
        currency.setCurrencyName("US Dollar");
        currency.setValidFrom(LocalDate.of(2026, 1, 1));
        currency.setValidTo(LocalDate.of(9999, 12, 31));

        currency.terminate(LocalDate.of(2026, 5, 31));

        assertThat(currency.getValidTo()).isEqualTo(LocalDate.of(2026, 5, 31));
        assertThat(currency.isValid(LocalDate.of(2026, 6, 1))).isFalse();
    }

    @Test
    void exchangeRateUsesCurrencyCodesInsteadOfCurrencyEntityReferences() {
        ExchangeRate exchangeRate = new ExchangeRate();
        exchangeRate.setFromCurrencyCode("USD");
        exchangeRate.setToCurrencyCode("KRW");

        assertThat(exchangeRate.getFromCurrencyCode()).isEqualTo("USD");
        assertThat(exchangeRate.getToCurrencyCode()).isEqualTo("KRW");
        assertThat(Arrays.stream(ExchangeRate.class.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(ManyToOne.class))
                .map(Field::getName))
                .isEmpty();
    }
}
