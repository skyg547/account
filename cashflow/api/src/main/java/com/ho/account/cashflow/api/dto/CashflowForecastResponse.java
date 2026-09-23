package com.ho.account.cashflow.api.dto;

import com.ho.account.cashflow.core.domain.CashflowForecast;
import com.ho.account.cashflow.core.domain.CashflowRiskLevel;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CashflowForecastResponse(
        String forecastId,
        LocalDate forecastDate,
        LocalDate targetDate,
        BigDecimal inflowEstimate,
        BigDecimal outflowEstimate,
        BigDecimal netLiquidity,
        CashflowRiskLevel riskLevel,
        String notes) {

    public static CashflowForecastResponse from(CashflowForecast forecast) {
        return new CashflowForecastResponse(
                forecast.getForecastId(),
                forecast.getForecastDate(),
                forecast.getTargetDate(),
                forecast.getInflowEstimate(),
                forecast.getOutflowEstimate(),
                forecast.getNetLiquidity(),
                forecast.getRiskLevel(),
                forecast.getNotes());
    }
}
