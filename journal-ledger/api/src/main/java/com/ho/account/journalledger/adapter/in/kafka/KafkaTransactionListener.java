package com.ho.account.journalledger.adapter.in.kafka;

import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.application.port.in.KafkaJournalEventUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Kafka 트랜잭션 이벤트 리스너
 * 타 모듈(매입, 매출, 급여 등)에서 발생하는 경제적 사건(Event)을 수신하여
 * 룰 엔진을 통해 전표를 자동으로 생성합니다.
 */
@Slf4j
@Component
public class KafkaTransactionListener {

    private static final String KAFKA_MAKER = "service:journal-kafka-maker";

    private final KafkaJournalEventUseCase kafkaJournalEventUseCase;
    private final JournalUseCase compatibilityJournalUseCase;

    /** Production construction requires the durable Kafka completeness use case. */
    @Autowired
    public KafkaTransactionListener(KafkaJournalEventUseCase kafkaJournalEventUseCase) {
        this.kafkaJournalEventUseCase = kafkaJournalEventUseCase;
        this.compatibilityJournalUseCase = null;
    }

    /**
     * Source-compatible direct construction for older callers.
     *
     * <p>This path has no broker operation identity, so an unmatched event fails closed instead of
     * pretending that it was durably quarantined.</p>
     */
    public KafkaTransactionListener(JournalUseCase journalUseCase) {
        this.kafkaJournalEventUseCase = null;
        this.compatibilityJournalUseCase = java.util.Objects.requireNonNull(journalUseCase);
    }

    @KafkaListener(topics = "transaction-events", groupId = "journal-ledger-group")
    public void listenTransactionEvent(
            @Payload Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received transaction event: topic={}, partition={}, offset={}", topic, partition, offset);

        try {
            LocalDate accountingDate = accountingDate(event);
            Map<String, Object> trustedEvent = trustedEvent(event);
            var result = kafkaJournalEventUseCase.process(
                    new KafkaJournalEventUseCase.BrokerRecord(topic, partition, offset),
                    trustedEvent,
                    accountingDate);
            switch (result.disposition()) {
                case JOURNAL_CREATED -> log.info(
                        "Generated journal {} for Kafka record {}-{}@{}",
                        result.journalEntryId(), topic, partition, offset);
                case QUARANTINED -> log.warn(
                        "Quarantined unmatched journal event {} for Kafka record {}-{}@{}",
                        result.quarantineId(), topic, partition, offset);
                case ALREADY_QUARANTINED -> log.info(
                        "Kafka record {}-{}@{} already has quarantine {}",
                        topic, partition, offset, result.quarantineId());
            }
        } catch (RuntimeException e) {
            log.error("Failed to process transaction event at {}-{}@{}", topic, partition, offset, e);
            throw new RuntimeException("Kafka message processing failed, triggering retry/DLQ fallback", e);
        }
    }

    /** Legacy direct-call contract retained for audited-source regression and source compatibility. */
    public void listenTransactionEvent(Map<String, Object> event) {
        if (compatibilityJournalUseCase == null) {
            throw new IllegalStateException("Direct Kafka event calls require the compatibility constructor");
        }
        try {
            var generated = compatibilityJournalUseCase.createJournalEntryFromEvent(
                    trustedEvent(event), accountingDate(event));
            if (generated.isEmpty()) {
                throw new IllegalStateException(
                        "Unmatched Kafka event cannot be acknowledged without broker coordinates");
            }
        } catch (RuntimeException exception) {
            throw new RuntimeException("Kafka message processing failed, triggering retry/DLQ fallback", exception);
        }
    }

    private LocalDate accountingDate(Map<String, Object> event) {
        Object supplied = event.get("accountingDate");
        // A consumer retry can run after a calendar boundary; only the source's explicit date is stable.
        if (!(supplied instanceof String date) || date.isBlank()) {
            throw new IllegalArgumentException("Kafka event accountingDate must be an ISO-8601 date");
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Kafka event accountingDate must be an ISO-8601 date", exception);
        }
    }

    private Map<String, Object> trustedEvent(Map<String, Object> event) {
        Map<String, Object> trustedEvent = new java.util.HashMap<>(event);
        // Kafka has no Gateway headers; payload identity never outranks the listener service principal.
        trustedEvent.put("createdBy", KAFKA_MAKER);
        trustedEvent.put("auditUser", KAFKA_MAKER);
        return trustedEvent;
    }
}
