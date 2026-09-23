package com.ho.account.cashflow.core.infrastructure.memory;

import com.ho.account.cashflow.core.application.port.out.CashflowForecastPersistencePort;
import com.ho.account.cashflow.core.domain.CashflowForecast;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryCashflowForecastAdapter implements CashflowForecastPersistencePort {

    private final Map<String, CashflowForecast> forecasts = new ConcurrentHashMap<>();

    @Override
    public CashflowForecast save(CashflowForecast forecast) {
        forecasts.put(forecast.getForecastId(), forecast);
        return forecast;
    }

    @Override
    public Optional<CashflowForecast> findById(String forecastId) {
        return Optional.ofNullable(forecasts.get(forecastId));
    }
}
