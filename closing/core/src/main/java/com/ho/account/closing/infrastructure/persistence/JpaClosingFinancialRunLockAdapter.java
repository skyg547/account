package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.application.port.out.ClosingFinancialRunLockPort;
import jakarta.persistence.EntityNotFoundException;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** A separate row lock survives nested recorder commits and is released by database crash cleanup. */
@Component
public class JpaClosingFinancialRunLockAdapter implements ClosingFinancialRunLockPort {
    private final Semaphore connectionReservations;
    private final ClosingFinancialRunLockRepository locks;
    private final PlatformTransactionManager transactionManager;

    public JpaClosingFinancialRunLockAdapter(ClosingFinancialRunLockRepository locks,
                                              PlatformTransactionManager transactionManager,
                                              @Value("${spring.datasource.hikari.maximum-pool-size:10}") int poolSize) {
        if (poolSize < 2) throw new IllegalStateException("Financial run recovery requires at least two DB connections");
        // Each holder needs one connection for the lock plus one for REQUIRES_NEW history/manifest writes.
        this.connectionReservations = new Semaphore(poolSize / 2, true);
        this.locks = locks;
        this.transactionManager = transactionManager;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void ensureLockRow(String scope, String key) {
        locks.save(new ClosingFinancialRunLock(scope, key));
    }

    @Override
    public <T> T withExclusiveRun(String scope, String key, Supplier<T> action) {
        connectionReservations.acquireUninterruptibly();
        try {
            TransactionTemplate transaction = new TransactionTemplate(transactionManager);
            transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            return transaction.execute(status -> {
                locks.findByScopeAndExecutionKey(scope, key)
                        .orElseThrow(() -> new EntityNotFoundException("Financial run lock not found"));
                return action.get();
            });
        } finally {
            connectionReservations.release();
        }
    }
}
