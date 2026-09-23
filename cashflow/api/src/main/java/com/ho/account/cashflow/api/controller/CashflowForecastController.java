package com.ho.account.cashflow.api.controller;

import com.ho.account.cashflow.api.dto.CashflowForecastResponse;
import com.ho.account.cashflow.api.dto.ForecastLiquidityRequest;
import com.ho.account.cashflow.core.application.port.in.CashflowForecastUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/cashflow/forecasts")
public class CashflowForecastController {

    private final CashflowForecastUseCase forecastUseCase;

    public CashflowForecastController(CashflowForecastUseCase forecastUseCase) {
        this.forecastUseCase = forecastUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CashflowForecastResponse forecast(@Valid @RequestBody ForecastLiquidityRequest request) {
        return CashflowForecastResponse.from(forecastUseCase.forecast(request.toCommand()));
    }

    @GetMapping("/{forecastId}")
    public CashflowForecastResponse findById(@PathVariable String forecastId) {
        return forecastUseCase.findById(forecastId)
                .map(CashflowForecastResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "cashflow forecast not found"));
    }
}
