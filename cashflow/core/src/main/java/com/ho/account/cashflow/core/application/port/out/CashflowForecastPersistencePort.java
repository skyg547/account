package com.ho.account.cashflow.core.application.port.out;

import com.ho.account.cashflow.core.domain.CashflowForecast;

import java.util.Optional;

public interface CashflowForecastPersistencePort {

    CashflowForecast save(CashflowForecast forecast);

    Optional<CashflowForecast> findById(String forecastId);
}
