package com.ho.account.cashflow.core.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ForecastLiquidityCommand(
        String forecastId,
        LocalDate forecastDate,
        LocalDate targetDate,
        BigDecimal inflowEstimate,
        BigDecimal outflowEstimate,
        BigDecimal watchThreshold,
        BigDecimal criticalThreshold,
        String notes) {
}
