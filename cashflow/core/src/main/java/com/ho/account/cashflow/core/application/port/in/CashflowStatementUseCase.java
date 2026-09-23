package com.ho.account.cashflow.core.application.port.in;

import com.ho.account.cashflow.core.domain.CashflowStatement;

import java.util.Optional;

public interface CashflowStatementUseCase {

    CashflowStatement generate(GenerateStatementCommand command);

    Optional<CashflowStatement> findById(String statementId);
}
