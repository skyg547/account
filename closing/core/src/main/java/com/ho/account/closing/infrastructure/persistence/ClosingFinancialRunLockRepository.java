package com.ho.account.closing.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ClosingFinancialRunLockRepository extends JpaRepository<ClosingFinancialRunLock, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ClosingFinancialRunLock> findByScopeAndExecutionKey(String scope, String executionKey);
}
