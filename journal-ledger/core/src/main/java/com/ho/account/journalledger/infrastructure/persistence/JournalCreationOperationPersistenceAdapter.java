package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.journalledger.application.port.in.JournalCreationIdentity;
import com.ho.account.journalledger.application.port.out.JournalCreationOperationPersistencePort;
import com.ho.account.journalledger.domain.journal.domain.JournalCreationOperation;
import com.ho.account.journalledger.infrastructure.persistence.repository.JournalCreationOperationRepository;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** JPA adapter for the journal-creation operation registry. */
@Component
@RequiredArgsConstructor
public class JournalCreationOperationPersistenceAdapter
        implements JournalCreationOperationPersistencePort {

    private final JournalCreationOperationRepository repository;
    private final EntityManager entityManager;

    @Override
    public void acquireOperationLock(JournalCreationIdentity identity) {
        JournalCreationOperationWriteLock.lock(entityManager, repository, identity);
    }

    @Override
    public Optional<JournalCreationOperation> findByIdentity(JournalCreationIdentity identity) {
        JournalCreationOperationWriteLock.requireWriteTransaction(entityManager);
        return repository.findBySourceTypeAndSourceIdAndOperationKey(
                        identity.sourceType(), identity.sourceId(), identity.operationKey())
                .map(operation -> {
                    // A managed instance may predate the stripe wait. Refresh after locking so the
                    // fingerprint and linked journal always reflect the preceding committed owner.
                    entityManager.refresh(operation);
                    return operation;
                });
    }

    @Override
    public JournalCreationOperation save(JournalCreationOperation operation) {
        JournalCreationOperationWriteLock.requireWriteTransaction(entityManager);
        // The journal FK and operation identity must reach the database in the caller's transaction;
        // a uniqueness violation is propagated because PostgreSQL has already aborted that transaction.
        return repository.saveAndFlush(operation);
    }
}
