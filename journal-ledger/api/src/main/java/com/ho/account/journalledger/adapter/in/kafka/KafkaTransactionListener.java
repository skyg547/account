package com.ho.account.journalledger.adapter.in.kafka;

import com.ho.account.journalledger.application.service.journal.JournalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/**
 * Kafka ?¸ëœ??…˜ ?´ë²¤??ë¦¬ìŠ¤??
 * ?€ ëª¨ë“ˆ(ë§¤ì…, ë§¤ì¶œ, ?€ì¶????ì„œ ë°œìƒ?˜ëŠ” ê²½ì œ???¬ê±´(Event)???˜ì‹ ?˜ì—¬
 * ë£??”ì§„???µí•´ ?„í‘œë¥??ë™?¼ë¡œ ?ì„±?©ë‹ˆ??
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
            // ?´ë²¤?¸ì—???Œê³„?¼ì ì¶”ì¶œ (?†ìœ¼ë©??¤ëŠ˜ ? ì§œ)
            LocalDate accountingDate = LocalDate.now();
            if (event.containsKey("accountingDate")) {
                accountingDate = LocalDate.parse(event.get("accountingDate").toString());
            }

            // ?„í‘œ ?ë™ ?ì„± ?œë„
            journalService.createJournalEntryFromEvent(event, accountingDate)
                    .ifPresentOrElse(
                            entry -> log.info("Successfully generated journal entry: No={}, ID={}", entry.getSlipNo(), entry.getId()),
                            () -> log.warn("No matching journal rule found for event: {}", event)
                    );

        } catch (Exception e) {
            log.error("Failed to process transaction event: {}", event, e);
            // TODO: Error Handling (Dead Letter Queue ??
        }
    }
}
