package com.ho.account.cashflow.api.dto;

import com.ho.account.cashflow.core.application.port.in.GenerateStatementCommand;
import com.ho.account.cashflow.core.domain.CashflowMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record GenerateCashflowStatementRequest(
        @NotBlank String statementId,
        @Min(1900) @Max(9999) int fiscalYear,
        @Min(1) @Max(12) int fiscalPeriod,
        @NotNull CashflowMethod method,
        @NotBlank String currency,
        @NotNull LocalDateTime generatedAt,
        // @Valid skips null elements, so reject them before converting line items.
        @NotNull List<@NotNull @Valid CashflowLineItemRequest> lineItems) {

    public GenerateStatementCommand toCommand() {
        return new GenerateStatementCommand(
                statementId,
                fiscalYear,
                fiscalPeriod,
                method,
                currency,
                generatedAt,
                lineItems.stream().map(CashflowLineItemRequest::toDomain).toList());
    }
}
