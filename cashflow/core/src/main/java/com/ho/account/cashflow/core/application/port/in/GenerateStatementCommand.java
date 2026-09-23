package com.ho.account.cashflow.core.application.port.in;

import com.ho.account.cashflow.core.domain.CashflowLineItem;
import com.ho.account.cashflow.core.domain.CashflowMethod;

import java.time.LocalDateTime;
import java.util.List;

public record GenerateStatementCommand(
        String statementId,
        int fiscalYear,
        int fiscalPeriod,
        CashflowMethod method,
        String currency,
        LocalDateTime generatedAt,
        List<CashflowLineItem> lineItems) {
}
