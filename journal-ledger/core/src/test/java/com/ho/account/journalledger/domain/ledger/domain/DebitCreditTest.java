package com.ho.account.journalledger.domain.ledger.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DebitCreditTest {

    @Test
    @DisplayName("차변과 대변은 DECIMAL(19,2) 계약으로 정규화된 서로 다른 불변 타입이다.")
    void normalizesDebitAndCreditWithoutLosingTheirSide() {
        Debit debit = Debit.of(new BigDecimal("100"));
        Credit credit = Credit.of(new BigDecimal("40.5"));

        assertThat(debit.amount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(credit.amount()).isEqualTo(new BigDecimal("40.50"));
        assertThat(debit.add(Debit.of(new BigDecimal("0.25"))).amount())
                .isEqualTo(new BigDecimal("100.25"));
    }

    @Test
    @DisplayName("원장 경계는 소수 센트, 음수, 전체 자릿수 초과를 무음 반올림하지 않고 거부한다.")
    void rejectsValuesThatCannotBeStoredExactly() {
        assertThatThrownBy(() -> Debit.of(new BigDecimal("1.001")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("소수점 2자리");
        assertThatThrownBy(() -> Credit.of(new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("음수");
        assertThatThrownBy(() -> Debit.of(new BigDecimal("100000000000000000.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("19자리");
    }

    @Test
    @DisplayName("환율은 소수 여덟 자리까지 보존하고 초과 정밀도는 거부한다.")
    void appliesIndependentExchangeRatePrecision() {
        assertThat(AccountingPrecision.exchangeRate(new BigDecimal("1350.12345678")))
                .isEqualTo(new BigDecimal("1350.12345678"));
        assertThatThrownBy(() ->
                AccountingPrecision.exchangeRate(new BigDecimal("1350.123456789")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("소수점 8자리");
    }
}
