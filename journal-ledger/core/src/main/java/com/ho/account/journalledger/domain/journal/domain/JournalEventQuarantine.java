package com.ho.account.journalledger.domain.journal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Durable exception record for a valid Kafka economic event that has no applicable journal rule.
 *
 * <p>The broker coordinate is the operation identity. It deliberately does not reuse journal
 * lineage because one source document may legitimately produce multiple journals.</p>
 */
@Entity
@Table(
        name = "journal_event_quarantine",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_journal_event_quarantine_record",
                columnNames = {"source_topic", "source_partition", "source_offset"}),
        indexes = @Index(
                name = "idx_journal_event_quarantine_completeness",
                columnList = "status, first_seen_at"))
public class JournalEventQuarantine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_topic", nullable = false, length = 249, updatable = false)
    private String sourceTopic;

    @Column(name = "source_partition", nullable = false, updatable = false)
    private int sourcePartition;

    @Column(name = "source_offset", nullable = false, updatable = false)
    private long sourceOffset;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String payloadJson;

    @Column(name = "accounting_date", nullable = false, updatable = false)
    private LocalDate accountingDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JournalEventQuarantineStatus status;

    @Column(name = "reason_code", nullable = false, length = 50)
    private String reasonCode;

    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private Instant firstSeenAt;

    @Column(name = "last_replay_at")
    private Instant lastReplayAt;

    @Column(name = "last_replay_actor", length = 100)
    private String lastReplayActor;

    @Column(name = "replay_attempts", nullable = false)
    private int replayAttempts;

    @Version
    @Column(nullable = false)
    private long version;

    protected JournalEventQuarantine() {
    }

    public static JournalEventQuarantine unmatched(
            String sourceTopic,
            int sourcePartition,
            long sourceOffset,
            String payloadJson,
            LocalDate accountingDate,
            Instant firstSeenAt) {
        if (sourceTopic == null || sourceTopic.isBlank()) {
            throw new IllegalArgumentException("Kafka source topic is required");
        }
        if (sourcePartition < 0 || sourceOffset < 0) {
            throw new IllegalArgumentException("Kafka partition and offset must be non-negative");
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new IllegalArgumentException("Quarantined event payload is required");
        }
        JournalEventQuarantine event = new JournalEventQuarantine();
        event.sourceTopic = sourceTopic.trim();
        event.sourcePartition = sourcePartition;
        event.sourceOffset = sourceOffset;
        event.payloadJson = payloadJson;
        event.accountingDate = java.util.Objects.requireNonNull(accountingDate, "accountingDate");
        event.status = JournalEventQuarantineStatus.QUARANTINED;
        event.reasonCode = "NO_MATCHING_RULE";
        event.firstSeenAt = java.util.Objects.requireNonNull(firstSeenAt, "firstSeenAt");
        return event;
    }

    public void noteReplayAttempt(String actor, Instant attemptedAt) {
        requireQuarantined();
        replayAttempts++;
        lastReplayActor = JournalActor.canonicalize(actor);
        lastReplayAt = java.util.Objects.requireNonNull(attemptedAt, "attemptedAt");
    }

    public void markReplayed(Long journalEntryId) {
        requireQuarantined();
        if (journalEntryId == null || journalEntryId < 1) {
            throw new IllegalArgumentException("A persisted journal ID is required to resolve quarantine");
        }
        this.journalEntryId = journalEntryId;
        this.status = JournalEventQuarantineStatus.REPLAYED;
    }

    private void requireQuarantined() {
        if (status != JournalEventQuarantineStatus.QUARANTINED) {
            throw new IllegalStateException("Replayed event quarantine is immutable");
        }
    }

    public Long getId() { return id; }
    public String getSourceTopic() { return sourceTopic; }
    public int getSourcePartition() { return sourcePartition; }
    public long getSourceOffset() { return sourceOffset; }
    public String getPayloadJson() { return payloadJson; }
    public LocalDate getAccountingDate() { return accountingDate; }
    public JournalEventQuarantineStatus getStatus() { return status; }
    public String getReasonCode() { return reasonCode; }
    public Long getJournalEntryId() { return journalEntryId; }
    public Instant getFirstSeenAt() { return firstSeenAt; }
    public Instant getLastReplayAt() { return lastReplayAt; }
    public String getLastReplayActor() { return lastReplayActor; }
    public int getReplayAttempts() { return replayAttempts; }
    public long getVersion() { return version; }
}
