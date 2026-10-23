package com.ho.account.journalledger.application.port.out;

import com.ho.account.journalledger.domain.journal.domain.JournalEventQuarantine;
import com.ho.account.journalledger.domain.journal.domain.JournalEventQuarantineStatus;
import java.util.List;
import java.util.Optional;

/** Persistence boundary for unmatched Kafka event completeness records. */
public interface JournalEventQuarantinePort {
    JournalEventQuarantine save(JournalEventQuarantine event);

    Optional<JournalEventQuarantine> findByBrokerRecord(String topic, int partition, long offset);

    Optional<JournalEventQuarantine> findByIdForUpdate(Long id);

    List<JournalEventQuarantine> findOldestByStatus(JournalEventQuarantineStatus status, int limit);

    CompletenessSnapshot summarizeCompleteness();

    record CompletenessSnapshot(
            long quarantinedCount,
            long replayedCount,
            Optional<java.time.Instant> oldestUnresolvedAt) {
    }
}
