package com.ho.account.masterdata.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProductPriceTest {

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.0000", "1.2345", "999999999999999.9999", "1.230000"})
    void acceptsExactlyRepresentableNonnegativePrices(String input) {
        Product product = new Product();
        product.setPrice(new BigDecimal(input));

        assertThat(product.getPrice()).isEqualByComparingTo(input);
        assertThat(product.getPrice().setScale(4)).isEqualByComparingTo(input);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.0001", "-1", "1.23456", "999999999999999.99991", "1000000000000000", "1000000000000000.0000"})
    void rejectsNegativeLossyOrOverflowingPrices(String input) {
        Product product = new Product();
        product.setPrice(BigDecimal.ONE);

        assertThatThrownBy(() -> product.setPrice(new BigDecimal(input)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(product.getPrice()).isEqualByComparingTo(BigDecimal.ONE);
    }
}
