package com.ho.account.cashflow.core.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CashflowForecastTest {

    private static final LocalDate FORECAST_DATE = LocalDate.of(2026, 9, 23);
    private static final LocalDate TARGET_DATE = LocalDate.of(2026, 10, 31);

    @Test
    void calculatesNormalWatchAndCriticalRiskAtConfiguredThresholds() {
        assertThat(forecast("normal", "1000", "900").getRiskLevel()).isEqualTo(CashflowRiskLevel.NORMAL);
        assertThat(forecast("watch", "900", "950").getRiskLevel()).isEqualTo(CashflowRiskLevel.WATCH);
        assertThat(forecast("critical", "800", "950").getRiskLevel()).isEqualTo(CashflowRiskLevel.CRITICAL);
    }

    @Test
    void assignsBoundaryValuesToTheLessSevereBand() {
        assertThat(forecast("watch-boundary", "900", "900").getRiskLevel())
                .isEqualTo(CashflowRiskLevel.NORMAL);
        assertThat(forecast("critical-boundary", "800", "900").getRiskLevel())
                .isEqualTo(CashflowRiskLevel.WATCH);
    }

    @Test
    void calculatesScaledNetLiquidityAndRejectsInvalidThresholdOrdering() {
        CashflowForecast forecast = forecast("scaled", "1000", "950.5");
        assertThat(forecast.getNetLiquidity()).isEqualByComparingTo("49.50");
        assertThat(forecast.getNetLiquidity().scale()).isEqualTo(2);

        assertThatThrownBy(() -> CashflowForecast.create(
                "invalid", FORECAST_DATE, TARGET_DATE,
                BigDecimal.TEN, BigDecimal.ZERO,
                new BigDecimal("-100"), BigDecimal.ZERO, "invalid ordering"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("criticalThreshold");
    }

    private static CashflowForecast forecast(String id, String inflow, String outflow) {
        return CashflowForecast.create(
                id,
                FORECAST_DATE,
                TARGET_DATE,
                new BigDecimal(inflow),
                new BigDecimal(outflow),
                BigDecimal.ZERO,
                new BigDecimal("-100"),
                "threshold test");
    }
}
