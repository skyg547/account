package com.ho.account.closing.application.port.out;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AllowanceBalanceTest {
    @Test
    void normalizesExplicitUnitsAndPreservesSignedExactAmounts() {
        var balance = new AllowanceBalance(" usd ", "eur", new BigDecimal("-20.000"), new BigDecimal("-25.0000"));
        assertThat(balance.transactionCurrencyCode()).isEqualTo("USD");
        assertThat(balance.functionalCurrencyCode()).isEqualTo("EUR");
        assertThat(balance.creditTransactionAmount()).isEqualTo(new BigDecimal("-20.00"));
        assertThat(balance.creditBaseAmount()).isEqualTo(new BigDecimal("-25.00"));
    }

    @ParameterizedTest
    @CsvSource({"100.001, 100.00", "100.00, 100.001", "100000000000000000, 0", "0, -100000000000000000"})
    void rejectsSilentRoundingOrOverflowInEitherUnit(String transaction, String base) {
        assertThatThrownBy(() -> new AllowanceBalance("USD", "KRW",
                new BigDecimal(transaction), new BigDecimal(base))).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "US", "EURO", "12$"})
    void bothCurrencyLabelsMustBeExplicitThreeLetterCodes(String currency) {
        assertThatThrownBy(() -> new AllowanceBalance(currency, "KRW", BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AllowanceBalance("USD", currency, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void neitherAmountMayBeNull() {
        assertThatThrownBy(() -> new AllowanceBalance("USD", "KRW", null, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AllowanceBalance("USD", "KRW", BigDecimal.ZERO, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
