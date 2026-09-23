package com.ho.account.cashflow.core.infrastructure.memory;

import com.ho.account.cashflow.core.application.port.out.CashflowStatementPersistencePort;
import com.ho.account.cashflow.core.domain.CashflowStatement;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryCashflowStatementAdapter implements CashflowStatementPersistencePort {

    private final Map<String, CashflowStatement> statements = new ConcurrentHashMap<>();

    @Override
    public CashflowStatement save(CashflowStatement statement) {
        statements.put(statement.getStatementId(), statement);
        return statement;
    }

    @Override
    public Optional<CashflowStatement> findById(String statementId) {
        return Optional.ofNullable(statements.get(statementId));
    }
}
