package com.ho.account.cashflow.api.controller;

import com.ho.account.cashflow.api.dto.CashflowStatementResponse;
import com.ho.account.cashflow.api.dto.GenerateCashflowStatementRequest;
import com.ho.account.cashflow.core.application.port.in.CashflowStatementUseCase;
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
@RequestMapping("/api/v1/cashflow/statements")
public class CashflowStatementController {

    private final CashflowStatementUseCase statementUseCase;

    public CashflowStatementController(CashflowStatementUseCase statementUseCase) {
        this.statementUseCase = statementUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CashflowStatementResponse generate(@Valid @RequestBody GenerateCashflowStatementRequest request) {
        return CashflowStatementResponse.from(statementUseCase.generate(request.toCommand()));
    }

    @GetMapping("/{statementId}")
    public CashflowStatementResponse findById(@PathVariable String statementId) {
        return statementUseCase.findById(statementId)
                .map(CashflowStatementResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "cashflow statement not found"));
    }
}
