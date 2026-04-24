package com.ho.account.journalledger.adapter.in.kafka;

import com.ho.account.journalledger.domain.journal.service.JournalService;
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

    private final JournalService journalService;

    @KafkaListener(topics = "transaction-events", groupId = "journal-ledger-group")
    public void listenTransactionEvent(Map<String, Object> event) {
        log.info("Received transaction event: {}", event);

        try {
            // 이벤트에서 회계일자 추출 (없으면 오늘 날짜)
            LocalDate accountingDate = LocalDate.now();
            if (event.containsKey("accountingDate")) {
                accountingDate = LocalDate.parse(event.get("accountingDate").toString());
            }

            // 전표 자동 생성 시도
            journalService.createJournalEntryFromEvent(event, accountingDate)
                    .ifPresentOrElse(
                            entry -> log.info("Successfully generated journal entry: No={}, ID={}", entry.getSlipNo(), entry.getId()),
                            () -> log.warn("No matching journal rule found for event: {}", event)
                    );

        } catch (Exception e) {
            log.error("Failed to process transaction event: {}", event, e);
            // TODO: Error Handling (Dead Letter Queue 등)
        }
    }
}
