package com.ho.account.shared.finance.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FinanceEnumsTest {

    @Test
    @DisplayName("CalculationStatus 열거형 값들이 올바르게 정의되어 있다")
    void testCalculationStatus() {
        assertThat(CalculationStatus.valueOf("READY")).isEqualTo(CalculationStatus.READY);
        assertThat(CalculationStatus.valueOf("RUNNING")).isEqualTo(CalculationStatus.RUNNING);
        assertThat(CalculationStatus.valueOf("COMPLETED")).isEqualTo(CalculationStatus.COMPLETED);
        assertThat(CalculationStatus.valueOf("FAILED")).isEqualTo(CalculationStatus.FAILED);
        assertThat(CalculationStatus.valueOf("SKIPPED")).isEqualTo(CalculationStatus.SKIPPED);
        assertThat(CalculationStatus.values()).hasSize(5);
    }

    @Test
    @DisplayName("CurrencyCode 주요 통화코드들이 정의되어 있다")
    void testCurrencyCode() {
        assertThat(CurrencyCode.valueOf("KRW")).isEqualTo(CurrencyCode.KRW);
        assertThat(CurrencyCode.valueOf("USD")).isEqualTo(CurrencyCode.USD);
        assertThat(CurrencyCode.valueOf("EUR")).isEqualTo(CurrencyCode.EUR);
        assertThat(CurrencyCode.values()).contains(CurrencyCode.KRW, CurrencyCode.USD, CurrencyCode.JPY);
    }

    @Test
    @DisplayName("CustomerType 및 ProductCategory 열거형 값들이 정의되어 있다")
    void testOtherEnums() {
        assertThat(CustomerType.values()).isNotEmpty();
        assertThat(ProductCategory.values()).isNotEmpty();
        assertThat(CrStaging.values()).isNotEmpty();
    }
}
