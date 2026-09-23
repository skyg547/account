package com.ho.account.cashflow.core.application.port.out;

import com.ho.account.cashflow.core.domain.CashflowStatement;

import java.util.Optional;

public interface CashflowStatementPersistencePort {

    CashflowStatement save(CashflowStatement statement);

    Optional<CashflowStatement> findById(String statementId);
}
