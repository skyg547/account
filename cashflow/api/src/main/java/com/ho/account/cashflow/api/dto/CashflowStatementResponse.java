package com.ho.account.cashflow.api.dto;

import com.ho.account.cashflow.core.domain.CashflowActivity;
import com.ho.account.cashflow.core.domain.CashflowLineItem;
import com.ho.account.cashflow.core.domain.CashflowMethod;
import com.ho.account.cashflow.core.domain.CashflowStatement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CashflowStatementResponse(
        String statementId,
        int fiscalYear,
        int fiscalPeriod,
        CashflowMethod method,
        String currency,
        LocalDateTime generatedAt,
        List<LineItemResponse> lineItems,
        BigDecimal totalOperating,
        BigDecimal totalInvesting,
        BigDecimal totalFinancing,
        BigDecimal netCashflow,
        BigDecimal beginningCash,
        BigDecimal endingCash) {

    public static CashflowStatementResponse from(CashflowStatement statement) {
        return new CashflowStatementResponse(
                statement.getStatementId(),
                statement.getFiscalYear(),
                statement.getFiscalPeriod(),
                statement.getMethod(),
                statement.getCurrency(),
                statement.getGeneratedAt(),
                statement.getLineItems().stream().map(LineItemResponse::from).toList(),
                statement.getTotalOperating(),
                statement.getTotalInvesting(),
                statement.getTotalFinancing(),
                statement.getNetCashflow(),
                statement.getBeginningCash(),
                statement.getEndingCash());
    }

    public record LineItemResponse(
            String lineCode,
            String category,
            CashflowActivity activity,
            BigDecimal amount,
            String currency,
            String description) {

        static LineItemResponse from(CashflowLineItem item) {
            return new LineItemResponse(
                    item.lineCode(), item.category(), item.activity(), item.amount(), item.currency(), item.description());
        }
    }
}
