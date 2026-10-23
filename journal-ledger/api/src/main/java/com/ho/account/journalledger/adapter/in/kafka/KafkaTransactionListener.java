package com.ho.account.journalledger.adapter.in.kafka;

import com.ho.account.journalledger.application.port.in.JournalCreationIdentity;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.application.port.in.KafkaJournalEventUseCase;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/** Receives Kafka events and delegates durable quarantine, journal creation, and replay ownership. */
@Slf4j
@Component
public class KafkaTransactionListener {

    private static final String KAFKA_MAKER = "service:journal-kafka-maker";

    private final KafkaJournalEventUseCase kafkaJournalEventUseCase;
    private final JournalUseCase compatibilityJournalUseCase;

    @Autowired
    public KafkaTransactionListener(KafkaJournalEventUseCase kafkaJournalEventUseCase) {
        this.kafkaJournalEventUseCase = kafkaJournalEventUseCase;
        this.compatibilityJournalUseCase = null;
    }

    /** Source-compatible direct construction for tests and embedded callers without broker coordinates. */
    public KafkaTransactionListener(JournalUseCase journalUseCase) {
        this.kafkaJournalEventUseCase = null;
        this.compatibilityJournalUseCase = Objects.requireNonNull(journalUseCase);
    }

    @KafkaListener(topics = "transaction-events", groupId = "journal-ledger-group")
    public void listenTransactionEvent(
            @Payload Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received transaction event: topic={}, partition={}, offset={}", topic, partition, offset);
        try {
            LocalDate date = accountingDate(event);
            JournalCreationIdentity identity = JournalCreationIdentity.fromEvent(event);
            var result = kafkaJournalEventUseCase.process(
                    new KafkaJournalEventUseCase.BrokerRecord(topic, partition, offset),
                    trustedEvent(event, identity), date);
            switch (result.disposition()) {
                case JOURNAL_CREATED -> log.info("Generated journal {} for Kafka record {}-{}@{}",
                        result.journalEntryId(), topic, partition, offset);
                case QUARANTINED -> log.warn("Quarantined unmatched journal event {} for Kafka record {}-{}@{}",
                        result.quarantineId(), topic, partition, offset);
                case ALREADY_QUARANTINED -> log.info("Kafka record {}-{}@{} already has quarantine {}",
                        topic, partition, offset, result.quarantineId());
            }
        } catch (RuntimeException e) {
            log.error("Failed to process transaction event at {}-{}@{}; failureType={}",
                    topic, partition, offset, e.getClass().getSimpleName());
            throw new RuntimeException("Kafka message processing failed, triggering retry/DLQ fallback", e);
        }
    }

    /** Legacy direct-call signature still requires a stable business key and explicit accounting date. */
    public void listenTransactionEvent(Map<String, Object> event) {
        if (compatibilityJournalUseCase == null) {
            throw new IllegalStateException("Direct Kafka event calls require the compatibility constructor");
        }
        try {
            LocalDate date = accountingDate(event);
            JournalCreationIdentity identity = JournalCreationIdentity.fromEvent(event);
            var generated = compatibilityJournalUseCase.createJournalEntryFromEvent(
                    identity, trustedEvent(event, identity), date);
            if (generated.isEmpty()) {
                throw new IllegalStateException("Unmatched Kafka event cannot be acknowledged without broker coordinates");
            }
        } catch (RuntimeException exception) {
            throw new RuntimeException("Kafka message processing failed, triggering retry/DLQ fallback", exception);
        }
    }

    private LocalDate accountingDate(Map<String, Object> event) {
        Object supplied = event.get("accountingDate");
        // A retry can cross midnight; a source-supplied accounting date is part of the request fingerprint.
        if (!(supplied instanceof String date) || date.isBlank()) {
            throw new IllegalArgumentException("Kafka event accountingDate must be an ISO-8601 date");
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Kafka event accountingDate must be an ISO-8601 date", exception);
        }
    }

    private Map<String, Object> trustedEvent(Map<String, Object> event, JournalCreationIdentity identity) {
        Map<String, Object> trusted = new HashMap<>(event);
        trusted.put("createdBy", KAFKA_MAKER);
        trusted.put("auditUser", KAFKA_MAKER);
        trusted.put("lineageSourceType", identity.sourceType());
        trusted.put("lineageSourceId", identity.sourceId());
        // Normalize both accepted key spellings before rules, quarantine and the v2 digest.
        trusted.put("operationKey", identity.operationKey());
        trusted.put("journalOperationKey", identity.operationKey());
        return trusted;
    }
}
