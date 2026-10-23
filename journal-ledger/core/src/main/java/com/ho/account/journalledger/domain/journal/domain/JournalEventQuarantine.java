package com.ho.account.journalledger.domain.journal.domain;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Durable exception record for a valid Kafka economic event that has no applicable journal rule.
 *
 * <p>The broker coordinate is the operation identity. It deliberately does not reuse journal
 * lineage because one source document may legitimately produce multiple journals.</p>
 */
public class JournalEventQuarantine {

    private Long id;

    private String sourceTopic;

    private int sourcePartition;

    private long sourceOffset;

    private String payloadJson;

    private LocalDate accountingDate;

    private JournalEventQuarantineStatus status;

    private String reasonCode;

    private Long journalEntryId;

    private Instant firstSeenAt;

    private Instant lastReplayAt;

    private String lastReplayActor;

    private int replayAttempts;

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
