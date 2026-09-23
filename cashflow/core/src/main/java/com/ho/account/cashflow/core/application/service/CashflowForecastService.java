package com.ho.account.cashflow.core.application.service;

import com.ho.account.cashflow.core.application.port.in.CashflowForecastUseCase;
import com.ho.account.cashflow.core.application.port.in.ForecastLiquidityCommand;
import com.ho.account.cashflow.core.application.port.out.CashflowForecastPersistencePort;
import com.ho.account.cashflow.core.domain.CashflowForecast;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class CashflowForecastService implements CashflowForecastUseCase {

    private final CashflowForecastPersistencePort forecastPersistencePort;

    public CashflowForecastService(CashflowForecastPersistencePort forecastPersistencePort) {
        this.forecastPersistencePort = forecastPersistencePort;
    }

    @Override
    public CashflowForecast forecast(ForecastLiquidityCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        CashflowForecast forecast = CashflowForecast.create(
                command.forecastId(),
                command.forecastDate(),
                command.targetDate(),
                command.inflowEstimate(),
                command.outflowEstimate(),
                command.watchThreshold(),
                command.criticalThreshold(),
                command.notes());
        return forecastPersistencePort.save(forecast);
    }

    @Override
    public Optional<CashflowForecast> findById(String forecastId) {
        return forecastPersistencePort.findById(forecastId);
    }
}
