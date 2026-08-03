package com.ho.account.deposit.service;

import com.ho.account.deposit.domain.DepositDayCountConvention;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DepositInterestAccrualCalculator 단위 테스트")
class DepositInterestAccrualCalculatorTest {

    private final DepositInterestAccrualCalculator calculator = new DepositInterestAccrualCalculator();

    @Test
    @DisplayName("10,000,000원, 연 3.5%, 30일 경과 시 ACTUAL_365 이자가 바르게 계산되는지 검증한다")
    void calculateDailyAccrual_Actual365() {
        BigDecimal balance = new BigDecimal("10000000.00");
        BigDecimal annualRate = new BigDecimal("0.0350"); // 3.5%
        long elapsedDays = 30;

        // Interest = 10,000,000 * 0.0350 * (30 / 365) = 28,767.1232...
        BigDecimal dailyAccrual = calculator.calculateDailyAccrual(
                balance, annualRate, elapsedDays, DepositDayCountConvention.ACTUAL_365);

        assertThat(dailyAccrual).isEqualByComparingTo("28767.1233");
    }

    @Test
    @DisplayName("10,000,000원, 연 3.5%, 30일 경과 시 ACTUAL_360 이자가 바르게 계산되는지 검증한다")
    void calculateDailyAccrual_Actual360() {
        BigDecimal balance = new BigDecimal("10000000.00");
        BigDecimal annualRate = new BigDecimal("0.0350"); // 3.5%
        long elapsedDays = 30;

        // Interest = 10,000,000 * 0.0350 * (30 / 360) = 29,166.6666...
        BigDecimal dailyAccrual = calculator.calculateDailyAccrual(
                balance, annualRate, elapsedDays, DepositDayCountConvention.ACTUAL_360);

        assertThat(dailyAccrual).isEqualByComparingTo("29166.6667");
    }

    @Test
    @DisplayName("유효하지 않은 입력값(음수 이율, 음수 경과일수)에 대해 예외가 발생한다")
    void calculateDailyAccrual_InvalidInputs() {
        BigDecimal balance = new BigDecimal("1000000.00");

        assertThatThrownBy(() -> calculator.calculateDailyAccrual(
                balance, new BigDecimal("-0.01"), 30, DepositDayCountConvention.ACTUAL_365))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("annualRate must be a positive decimal rate");

        assertThatThrownBy(() -> calculator.calculateDailyAccrual(
                balance, new BigDecimal("0.03"), -5, DepositDayCountConvention.ACTUAL_365))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("elapsedDays must not be negative");
    }
}
