package com.ho.account.journalledger.application.port.out;

import com.ho.account.journalledger.application.port.in.JournalCreationIdentity;
import com.ho.account.journalledger.domain.journal.domain.JournalCreationOperation;
import java.util.Optional;

/** Persistence boundary for the durable journal-creation idempotency operation. */
public interface JournalCreationOperationPersistencePort {

    /**
     * Serializes all transactions using the same identity, including the first transaction before
     * an operation row exists. The acquired lock must remain held until the caller transaction ends.
     */
    void acquireOperationLock(JournalCreationIdentity identity);

    Optional<JournalCreationOperation> findByIdentity(JournalCreationIdentity identity);

    JournalCreationOperation save(JournalCreationOperation operation);
}
