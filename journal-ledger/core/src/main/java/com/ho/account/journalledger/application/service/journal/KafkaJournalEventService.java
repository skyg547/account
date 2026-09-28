package com.ho.account.journalledger.application.service.journal;

import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.application.port.in.KafkaJournalEventUseCase;
import com.ho.account.journalledger.application.port.out.JournalEventPayloadCodec;
import com.ho.account.journalledger.application.port.out.JournalEventQuarantinePort;
import com.ho.account.journalledger.domain.journal.domain.JournalActor;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEventQuarantine;
import com.ho.account.journalledger.domain.journal.domain.JournalEventQuarantineStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates rule evaluation with durable no-match disposition and atomic replay. */
@Service
public class KafkaJournalEventService implements KafkaJournalEventUseCase {

    private final JournalUseCase journalUseCase;
    private final JournalEventQuarantinePort quarantinePort;
    private final JournalEventPayloadCodec payloadCodec;

    public KafkaJournalEventService(
            JournalUseCase journalUseCase,
            JournalEventQuarantinePort quarantinePort,
            JournalEventPayloadCodec payloadCodec) {
        this.journalUseCase = journalUseCase;
        this.quarantinePort = quarantinePort;
        this.payloadCodec = payloadCodec;
    }

    @Override
    @Transactional
    public ProcessingResult process(BrokerRecord record, Map<String, Object> event, LocalDate accountingDate) {
        var existing = quarantinePort.findByBrokerRecord(record.topic(), record.partition(), record.offset());
        if (existing.isPresent()) {
            JournalEventQuarantine quarantine = existing.get();
            return new ProcessingResult(
                    ProcessingResult.Disposition.ALREADY_QUARANTINED,
                    quarantine.getJournalEntryId(),
                    quarantine.getId());
        }

        var journal = journalUseCase.createJournalEntryFromEvent(event, accountingDate);
        if (journal.isPresent()) {
            return new ProcessingResult(
                    ProcessingResult.Disposition.JOURNAL_CREATED,
                    journal.get().getId(),
                    null);
        }

        JournalEventQuarantine quarantine = JournalEventQuarantine.unmatched(
                record.topic(),
                record.partition(),
                record.offset(),
                payloadCodec.encode(event),
                accountingDate,
                Instant.now());
        quarantine = quarantinePort.save(quarantine);
        return new ProcessingResult(
                ProcessingResult.Disposition.QUARANTINED,
                null,
                quarantine.getId());
    }

    @Override
    @Transactional
    public ReplayResult replay(Long quarantineId, String actor) {
        String canonicalActor = JournalActor.canonicalize(actor);
        JournalEventQuarantine quarantine = quarantinePort.findByIdForUpdate(quarantineId)
                .orElseThrow(() -> new JournalEventQuarantineNotFoundException(quarantineId));

        if (quarantine.getStatus() == JournalEventQuarantineStatus.REPLAYED) {
            JournalEntry existing = journalUseCase.getJournalEntryWithDetails(quarantine.getJournalEntryId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Replayed quarantine points to a missing journal: " + quarantine.getJournalEntryId()));
            return new ReplayResult(true, false, existing, quarantine);
        }

        quarantine.noteReplayAttempt(canonicalActor, Instant.now());
        var generated = journalUseCase.createJournalEntryFromEvent(
                payloadCodec.decode(quarantine.getPayloadJson()),
                quarantine.getAccountingDate());
        if (generated.isEmpty()) {
            quarantinePort.save(quarantine);
            return new ReplayResult(false, false, null, quarantine);
        }

        JournalEntry journal = generated.get();
        quarantine.markReplayed(journal.getId());
        quarantinePort.save(quarantine);
        return new ReplayResult(true, true, journal, quarantine);
    }

    @Override
    @Transactional(readOnly = true)
    public CompletenessSummary completenessSummary() {
        // One aggregate statement gives the three values one READ_COMMITTED statement snapshot.
        var snapshot = quarantinePort.summarizeCompleteness();
        return new CompletenessSummary(
                snapshot.quarantinedCount(),
                snapshot.replayedCount(),
                snapshot.oldestUnresolvedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JournalEventQuarantine> find(JournalEventQuarantineStatus status, int limit) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Quarantine query limit must be between 1 and 100");
        }
        return quarantinePort.findOldestByStatus(status, limit);
    }
}
