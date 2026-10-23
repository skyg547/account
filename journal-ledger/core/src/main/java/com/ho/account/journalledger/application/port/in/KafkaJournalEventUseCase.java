package com.ho.account.journalledger.application.port.in;

import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEventQuarantine;
import com.ho.account.journalledger.domain.journal.domain.JournalEventQuarantineStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Inbound contract for Kafka processing, completeness visibility, and controlled replay. */
public interface KafkaJournalEventUseCase {

    ProcessingResult process(BrokerRecord record, Map<String, Object> event, LocalDate accountingDate);

    ReplayResult replay(Long quarantineId, String actor);

    CompletenessSummary completenessSummary();

    List<JournalEventQuarantine> find(JournalEventQuarantineStatus status, int limit);

    record BrokerRecord(String topic, int partition, long offset) {
        public BrokerRecord {
            if (topic == null || topic.isBlank()) throw new IllegalArgumentException("Kafka topic is required");
            if (partition < 0 || offset < 0) {
                throw new IllegalArgumentException("Kafka partition and offset must be non-negative");
            }
            topic = topic.trim();
        }
    }

    record ProcessingResult(Disposition disposition, Long journalEntryId, Long quarantineId) {
        public enum Disposition { JOURNAL_CREATED, QUARANTINED, ALREADY_QUARANTINED }
    }

    record ReplayResult(boolean replayed, boolean newlyCreated, JournalEntry journalEntry,
                        JournalEventQuarantine quarantine) {
    }

    record CompletenessSummary(long quarantinedCount, long replayedCount, Optional<Instant> oldestUnresolvedAt) {
    }
}
