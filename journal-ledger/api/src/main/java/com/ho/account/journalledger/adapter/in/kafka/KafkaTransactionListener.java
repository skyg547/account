package com.ho.account.journalledger.adapter.in.kafka;

import com.ho.account.journalledger.application.port.in.JournalCreationIdentity;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/**
 * Kafka 트랜잭션 이벤트 리스너
 * 타 모듈(매입, 매출, 급여 등)에서 발생하는 경제적 사건(Event)을 수신하여
 * 룰 엔진을 통해 전표를 자동으로 생성합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaTransactionListener {

    private static final String KAFKA_MAKER = "service:journal-kafka-maker";

    private final JournalUseCase journalUseCase;

    @KafkaListener(topics = "transaction-events", groupId = "journal-ledger-group")
    public void listenTransactionEvent(Map<String, Object> event) {
        try {
            JournalCreationIdentity identity = creationIdentity(event);
            log.info("Received transaction event");

            // 이벤트에서 회계일자 추출 (없으면 오늘 날짜)
            LocalDate accountingDate = LocalDate.now();
            if (event.containsKey("accountingDate")) {
                accountingDate = LocalDate.parse(event.get("accountingDate").toString());
            }

            // 전표 자동 생성 시도
            Map<String, Object> trustedEvent = new java.util.HashMap<>(event);
            // Kafka has no Gateway headers; bind the entry to this listener's service principal.
            trustedEvent.put("createdBy", KAFKA_MAKER);
            trustedEvent.put("auditUser", KAFKA_MAKER);
            // The generated journal must persist the same lineage protected by the operation claim.
            trustedEvent.put("lineageSourceType", identity.sourceType());
            trustedEvent.put("lineageSourceId", identity.sourceId());
            journalUseCase.createJournalEntryFromEvent(identity, trustedEvent, accountingDate)
                    .ifPresentOrElse(
                            entry -> log.info("Successfully generated journal entry: No={}, ID={}", entry.getSlipNo(), entry.getId()),
                            () -> log.warn("No matching journal rule found for transaction event")
                    );

        } catch (Exception e) {
            // Do not log the event payload, validation values, or fingerprint material.
            log.error("Failed to process transaction event; failureType={}",
                    e.getClass().getSimpleName());
            // T29 fixed: Handle Kafka listener failures with a DLQ strategy. 
            // In a real Spring Kafka setup, this is often handled by DeadLetterPublishingRecoverer or a custom DLQ topic.
            // Here, we ensure the error is logged observability and potentially throw a specific exception to trigger 
            // Spring Kafka's built-in retry/DLQ mechanism instead of swallowing the exception.
            throw new RuntimeException("Kafka message processing failed, triggering retry/DLQ fallback", e);
        }
    }

    private JournalCreationIdentity creationIdentity(Map<String, Object> event) {
        String sourceType = firstStableScalar(event, "lineageSourceType", "eventType", "ruleCode");
        String sourceId = firstStableScalar(
                event, "lineageSourceId", "eventId", "sourceId", "documentId", "assetCode", "contractNo");
        String operationKey = firstStableScalar(event, "journalOperationKey", "operationKey");
        return new JournalCreationIdentity(sourceType, sourceId, operationKey);
    }

    private String firstStableScalar(Map<String, Object> event, String... keys) {
        for (String key : keys) {
            Object value = event.get(key);
            if (value == null) {
                continue;
            }
            if (!(value instanceof String) && !(value instanceof Number)) {
                throw new IllegalArgumentException(key + " must be a string or number");
            }
            String text = value.toString();
            if (!text.isBlank()) {
                return text;
            }
        }
        return null;
    }
}
