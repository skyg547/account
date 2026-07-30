package com.ho.account.budget.infrastructure.persistence;

import com.ho.account.budget.application.exception.BudgetConflictException;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;

/** Spring/JPA의 무결성·버전 예외가 application 경계 밖으로 새지 않게 번역합니다. */
final class BudgetPersistenceConflictTranslator {

    private BudgetPersistenceConflictTranslator() {
    }

    static <T> T translate(String message, Supplier<T> persistenceOperation) {
        try {
            return persistenceOperation.get();
        } catch (DataIntegrityViolationException | OptimisticLockingFailureException exception) {
            throw new BudgetConflictException(message, exception);
        }
    }
}
