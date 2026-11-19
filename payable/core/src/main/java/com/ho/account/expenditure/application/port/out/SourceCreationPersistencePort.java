package com.ho.account.expenditure.application.port.out;

import java.util.Optional;

/** Locks one immutable business request until its source transaction commits or rolls back. */
public interface SourceCreationPersistencePort {
    void acquireLock(String sourceType, String businessKey);
    Optional<Receipt> find(String sourceType, String businessKey);
    void save(String sourceType, String businessKey, String requestFingerprint, Long sourceId);
    record Receipt(String requestFingerprint, Long sourceId) { }
}
