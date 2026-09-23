package com.ho.account.cashflow.core.application.service;

import com.ho.account.cashflow.core.application.port.in.CashflowStatementUseCase;
import com.ho.account.cashflow.core.application.port.in.GenerateStatementCommand;
import com.ho.account.cashflow.core.application.port.out.CashflowStatementPersistencePort;
import com.ho.account.cashflow.core.application.port.out.LedgerCashBalancePort;
import com.ho.account.cashflow.core.domain.CashflowStatement;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

@Service
public class CashflowStatementService implements CashflowStatementUseCase {

    private final CashflowStatementPersistencePort statementPersistencePort;
    private final LedgerCashBalancePort ledgerCashBalancePort;

    public CashflowStatementService(
            CashflowStatementPersistencePort statementPersistencePort,
            LedgerCashBalancePort ledgerCashBalancePort) {
        this.statementPersistencePort = statementPersistencePort;
        this.ledgerCashBalancePort = ledgerCashBalancePort;
    }

    @Override
    public CashflowStatement generate(GenerateStatementCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        BigDecimal beginningCash = ledgerCashBalancePort.findBeginningCash(
                command.fiscalYear(), command.fiscalPeriod(), command.currency());
        CashflowStatement statement = CashflowStatement.generate(
                command.statementId(),
                command.fiscalYear(),
                command.fiscalPeriod(),
                command.method(),
                command.currency(),
                command.generatedAt(),
                command.lineItems(),
                beginningCash);
        return statementPersistencePort.save(statement);
    }

    @Override
    public Optional<CashflowStatement> findById(String statementId) {
        return statementPersistencePort.findById(statementId);
    }
}
