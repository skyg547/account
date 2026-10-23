package com.ho.account.journalledger.infrastructure.persistence.repository;

import com.ho.account.journalledger.domain.journal.domain.JournalCreationOperation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** JPA repository for durable journal-creation identities and their preseeded lock stripes. */
@Repository
public interface JournalCreationOperationRepository
        extends JpaRepository<JournalCreationOperation, Long> {

    Optional<JournalCreationOperation> findBySourceTypeAndSourceIdAndOperationKey(
            String sourceType, String sourceId, String operationKey);

    @Query(value = "SELECT lock_id FROM journal_creation_operation_locks "
            + "WHERE lock_id = :lockId FOR UPDATE", nativeQuery = true)
    Optional<Integer> lockOperationStripe(@Param("lockId") int lockId);
}
