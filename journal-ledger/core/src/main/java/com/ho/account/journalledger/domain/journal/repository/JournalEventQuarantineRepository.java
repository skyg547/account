package com.ho.account.journalledger.domain.journal.repository;

import com.ho.account.journalledger.domain.journal.domain.JournalEventQuarantine;
import com.ho.account.journalledger.domain.journal.domain.JournalEventQuarantineStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JournalEventQuarantineRepository extends JpaRepository<JournalEventQuarantine, Long> {

    Optional<JournalEventQuarantine> findBySourceTopicAndSourcePartitionAndSourceOffset(
            String sourceTopic, int sourcePartition, long sourceOffset);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT event FROM JournalEventQuarantine event WHERE event.id = :id")
    Optional<JournalEventQuarantine> findByIdForUpdate(@Param("id") Long id);

    List<JournalEventQuarantine> findByStatusOrderByFirstSeenAtAsc(
            JournalEventQuarantineStatus status, Pageable pageable);

    /** One SQL statement prevents mixed completeness values across separate READ_COMMITTED snapshots. */
    @Query(value = """
            SELECT
                SUM(CASE WHEN status = 'QUARANTINED' THEN 1 ELSE 0 END) AS quarantinedCount,
                SUM(CASE WHEN status = 'REPLAYED' THEN 1 ELSE 0 END) AS replayedCount,
                MIN(CASE WHEN status = 'QUARANTINED' THEN first_seen_at ELSE NULL END)
                    AS oldestUnresolvedAt
            FROM journal_event_quarantine
            """, nativeQuery = true)
    CompletenessProjection summarizeCompleteness();

    interface CompletenessProjection {
        Long getQuarantinedCount();
        Long getReplayedCount();
        java.time.OffsetDateTime getOldestUnresolvedAt();
    }
}
