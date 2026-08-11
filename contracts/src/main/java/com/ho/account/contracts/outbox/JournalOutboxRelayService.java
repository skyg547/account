package com.ho.account.contracts.outbox;

import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * [Transactional Outbox 패턴 - 전표 비동기 릴레이 엔진 (JournalOutboxRelayService)]
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 로컬 DB의 Outbox 저장소에 기록된 PENDING 상태의 전표 이벤트를 읽어서 
 * 실제 전표 서비스({@link JournalPostingPort})로 비동기 전달하는 릴레이(Relay / Polling Publisher) 서비스입니다.
 * 
 * **작동 원리 (Workflow):**
 * 1. 로컬 서비스(Deposit, Loan 등)는 로컬 DB 트랜잭션 내에서 `JournalOutboxEvent`를 저장합니다.
 * 2. 이 릴레이 서비스가 `publishPendingJournalEvents()`를 통해 PENDING 상태의 이벤트를 순차적으로 조회합니다.
 * 3. 외부 전표 포트(`JournalPostingPort.createDraftEntry`)를 호출하여 전표 생성을 시도합니다.
 * 4. 전표 생성이 성공하면 Outbox 상태를 `PUBLISHED`로 변경합니다.
 * 5. 외부 통신 장애나 시스템 오류 발생 시 `FAILED` 처리 및 재시도 카운트를 증가시켜 
 *    로컬 비즈니스 DB의 데이터와 전표 서비스 데이터 간의 **최종 정합성(Eventual Consistency)**을 보장합니다.
 */
public class JournalOutboxRelayService implements OutboxEventPublisher {

    private static final Logger log = Logger.getLogger(JournalOutboxRelayService.class.getName());

    private final OutboxPort outboxPort;
    private final JournalPostingPort journalPostingPort;

    public JournalOutboxRelayService(OutboxPort outboxPort, JournalPostingPort journalPostingPort) {
        this.outboxPort = Objects.requireNonNull(outboxPort, "outboxPort must not be null");
        this.journalPostingPort = Objects.requireNonNull(journalPostingPort, "journalPostingPort must not be null");
    }

    @Override
    public int publishPendingJournalEvents() {
        List<JournalOutboxEvent> pendingEvents = outboxPort.findPendingJournalEvents(100);
        int successCount = 0;

        for (JournalOutboxEvent event : pendingEvents) {
            if (publish(event)) {
                successCount++;
            }
        }
        return successCount;
    }

    @Override
    public boolean publish(JournalOutboxEvent event) {
        if (event == null || event.getStatus() != OutboxStatus.PENDING) {
            return false;
        }

        try {
            log.info(() -> String.format("[OutboxRelay] Publishing journal entry event. IdempotencyKey: %s, Module: %s",
                    event.getIdempotencyKey(), event.getSourceModule()));

            // 외부 전표 시스템으로 동기/비동기 API 요청 수행
            JournalPostingResult result = journalPostingPort.createDraftEntry(event.getCommand());

            if (result != null && result.journalEntryId() != null) {
                outboxPort.markJournalEventAsPublished(event.getEventId(), LocalDateTime.now());
                log.info(() -> String.format("[OutboxRelay] Successfully published event %s. SlipNo: %s",
                        event.getEventId(), result.slipNo()));
                return true;
            } else {
                String error = "JournalPostingPort returned null result or null journalEntryId";
                outboxPort.markJournalEventAsFailed(event.getEventId(), error);
                log.warning(() -> String.format("[OutboxRelay] Failed to publish event %s: %s", event.getEventId(), error));
                return false;
            }
        } catch (Exception ex) {
            outboxPort.markJournalEventAsFailed(event.getEventId(), ex.getMessage());
            log.severe(() -> String.format("[OutboxRelay] Exception occurred during publishing event %s: %s",
                    event.getEventId(), ex.getMessage()));
            return false;
        }
    }
}
