package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.journalledger.application.port.out.JournalEventQuarantinePort;
import com.ho.account.journalledger.domain.journal.domain.JournalEventQuarantine;
import com.ho.account.journalledger.domain.journal.domain.JournalEventQuarantineStatus;
import com.ho.account.journalledger.infrastructure.persistence.repository.JournalEventQuarantineRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/** JPA output adapter for the durable unmatched-event control table. */
@Component
@RequiredArgsConstructor
public class JournalEventQuarantinePersistenceAdapter implements JournalEventQuarantinePort {

    private final JournalEventQuarantineRepository repository;

    @Override
    public JournalEventQuarantine save(JournalEventQuarantine event) {
        return repository.save(event);
    }

    @Override
    public Optional<JournalEventQuarantine> findByBrokerRecord(String topic, int partition, long offset) {
        return repository.findBySourceTopicAndSourcePartitionAndSourceOffset(topic, partition, offset);
    }

    @Override
    public Optional<JournalEventQuarantine> findByIdForUpdate(Long id) {
        return repository.findByIdForUpdate(id);
    }

    @Override
    public List<JournalEventQuarantine> findOldestByStatus(
            JournalEventQuarantineStatus status, int limit) {
        return repository.findByStatusOrderByFirstSeenAtAsc(status, PageRequest.of(0, limit));
    }

    @Override
    public CompletenessSnapshot summarizeCompleteness() {
        var projection = repository.summarizeCompleteness();
        return new CompletenessSnapshot(
                projection.getQuarantinedCount() == null ? 0 : projection.getQuarantinedCount(),
                projection.getReplayedCount() == null ? 0 : projection.getReplayedCount(),
                Optional.ofNullable(projection.getOldestUnresolvedAt())
                        .map(java.time.OffsetDateTime::toInstant));
    }
}
