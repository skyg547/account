package com.ho.account.cashflow.api.dto;

import com.ho.account.cashflow.core.application.port.in.ForecastLiquidityCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ForecastLiquidityRequest(
        @NotBlank String forecastId,
        @NotNull LocalDate forecastDate,
        @NotNull LocalDate targetDate,
        @NotNull @PositiveOrZero BigDecimal inflowEstimate,
        @NotNull @PositiveOrZero BigDecimal outflowEstimate,
        @NotNull BigDecimal watchThreshold,
        @NotNull BigDecimal criticalThreshold,
        String notes) {

    public ForecastLiquidityCommand toCommand() {
        return new ForecastLiquidityCommand(
                forecastId,
                forecastDate,
                targetDate,
                inflowEstimate,
                outflowEstimate,
                watchThreshold,
                criticalThreshold,
                notes);
    }
}
