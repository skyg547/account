package com.ho.account.cashflow.core.application.port.in;

import com.ho.account.cashflow.core.domain.CashflowForecast;

import java.util.Optional;

public interface CashflowForecastUseCase {

    CashflowForecast forecast(ForecastLiquidityCommand command);

    Optional<CashflowForecast> findById(String forecastId);
}
