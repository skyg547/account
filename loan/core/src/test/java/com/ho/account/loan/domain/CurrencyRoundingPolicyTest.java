package com.ho.account.loan.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyRoundingPolicyTest {

    @Test
    @DisplayName("KRW 통화는 1원 미만 절사(소수점 0자리, FLOOR) 규칙이 적용되어야 한다")
    void testKrwRounding() {
        CurrencyRoundingPolicy policy = CurrencyRoundingPolicy.of("KRW");

        assertThat(policy.getScale()).isEqualTo(0);
        assertThat(policy.applyRounding(new BigDecimal("1000.99"))).isEqualTo(new BigDecimal("1000"));
        assertThat(policy.applyRounding(new BigDecimal("1000.01"))).isEqualTo(new BigDecimal("1000"));
        assertThat(policy.applyRounding(new BigDecimal("500.50"))).isEqualTo(new BigDecimal("500"));
    }

    @Test
    @DisplayName("JPY 통화는 1엔 미만 절사(소수점 0자리, FLOOR) 규칙이 적용되어야 한다")
    void testJpyRounding() {
        CurrencyRoundingPolicy policy = CurrencyRoundingPolicy.of("JPY");

        assertThat(policy.getScale()).isEqualTo(0);
        assertThat(policy.applyRounding(new BigDecimal("1250.75"))).isEqualTo(new BigDecimal("1250"));
    }

    @Test
    @DisplayName("USD 및 EUR 통화는 Cents 단위 반올림(소수점 2자리, HALF_UP) 규칙이 적용되어야 한다")
    void testUsdAndEurRounding() {
        CurrencyRoundingPolicy usdPolicy = CurrencyRoundingPolicy.of("USD");
        CurrencyRoundingPolicy eurPolicy = CurrencyRoundingPolicy.of("EUR");

        assertThat(usdPolicy.getScale()).isEqualTo(2);
        assertThat(usdPolicy.applyRounding(new BigDecimal("100.125"))).isEqualTo(new BigDecimal("100.13"));
        assertThat(usdPolicy.applyRounding(new BigDecimal("100.124"))).isEqualTo(new BigDecimal("100.12"));

        assertThat(eurPolicy.getScale()).isEqualTo(2);
        assertThat(eurPolicy.applyRounding(new BigDecimal("250.995"))).isEqualTo(new BigDecimal("251.00"));
    }

    @ParameterizedTest
    @CsvSource({
            "'', KRW",
            "null, KRW",
            "INVALID, KRW",
            "usd, USD",
            "krw, KRW",
            "eur, EUR",
            "jpy, JPY"
    })
    @DisplayName("소문자나 Null/잘못된 통화코드 입력 시 알맞은 정책 및 Fallback(KRW)이 반환된다")
    void testPolicyOf(String inputCode, String expectedEnumName) {
        String code = "null".equals(inputCode) ? null : inputCode;
        CurrencyRoundingPolicy policy = CurrencyRoundingPolicy.of(code);
        assertThat(policy.name()).isEqualTo(expectedEnumName);
    }
}
