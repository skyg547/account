package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.journalledger.application.port.in.JournalCreationIdentity;
import com.ho.account.journalledger.infrastructure.persistence.repository.JournalCreationOperationRepository;
import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.util.Objects;
import org.hibernate.Session;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Serializes the first and subsequent uses of one journal-creation operation identity. */
final class JournalCreationOperationWriteLock {

    private static final int STRIPE_COUNT = 256;

    private JournalCreationOperationWriteLock() {
    }

    static void lock(
            EntityManager entityManager,
            JournalCreationOperationRepository repository,
            JournalCreationIdentity identity) {
        requireWriteTransaction(entityManager);
        JournalCreationIdentity requiredIdentity = Objects.requireNonNull(identity, "identity");

        // Preserve earlier journal writes before waiting. Once the stripe is held, READ_COMMITTED
        // lets the caller re-read the operation row committed by the preceding owner.
        entityManager.flush();
        int lockId = stableStripe(requiredIdentity);
        Integer acquiredLockId = repository.lockOperationStripe(lockId)
                .orElseThrow(() -> new IllegalStateException(
                        "Journal creation lock stripe is missing; apply migration V20"));
        if (acquiredLockId != lockId) {
            throw new IllegalStateException("Unexpected journal creation lock stripe: " + acquiredLockId);
        }
    }

    static void requireWriteTransaction(EntityManager entityManager) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            throw new IllegalStateException(
                    "Journal creation operations require an active write transaction");
        }
        int isolation = entityManager.unwrap(Session.class)
                .doReturningWork(Connection::getTransactionIsolation);
        // A snapshot established before the stripe wait cannot be refreshed reliably at stricter
        // isolation levels, so reject it before persisting an idempotency decision.
        if (isolation != Connection.TRANSACTION_READ_COMMITTED) {
            throw new IllegalStateException(
                    "Journal creation operations require READ_COMMITTED transaction isolation");
        }
    }

    private static int stableStripe(JournalCreationIdentity identity) {
        // String.hashCode is specified by Java and therefore stable across JVM instances. Folding
        // all three bounded components keeps operationKey-distinct journals independently keyed.
        int hash = identity.sourceType().hashCode();
        hash = 31 * hash + identity.sourceId().hashCode();
        hash = 31 * hash + identity.operationKey().hashCode();
        return Math.floorMod(hash, STRIPE_COUNT);
    }
}
