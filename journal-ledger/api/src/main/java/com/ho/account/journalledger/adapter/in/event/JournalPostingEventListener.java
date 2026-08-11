package com.ho.account.journalledger.adapter.in.event;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * [헥사고날 아키텍처 - 전표 전기 비동기 이벤트 인바운드 어댑터 (JournalPostingEventListener)]
 *
 * ───────────────────────────────────────────────────────────────────────────────────
 * 🐣 [초보자를 위한 아키텍처 및 MSA 전환 교육용 주석 (Pedagogical Comments)]
 *
 * 1. MSA 비동기 이벤트 기반 통신(Event-Driven Architecture) 및 Transactional Outbox 연계:
 *    - 동기 HTTP REST 호출 방식은 호출 측과 수신 측 간 가용성 결합(Availability Coupling)을 형성하여,
 *      `journal-ledger` 서비스 장애 시 타 서비스(Payable/Loan 등)의 트랜잭션까지 동반 실패하는 가속 장애(Cascading Failure)가 발생할 수 있습니다.
 *    - 이를 해결하기 위해 타 모듈은 Transactional Outbox 패턴을 사용하여 local DB 트랜잭션 완료 후 비동기 이벤트를 발행합니다.
 *    - 본 이벤트 리스너(`JournalPostingEventListener`)는 이벤트를 수신하는 Async Event Inbound Adapter로서,
 *      비동기 환경에서도 안정적으로 전표를 생성하고 최종 정합성(Eventual Consistency)을 달성하도록 돕습니다.
 *
 * 2. 멱등성(Idempotency)과 최종 정합성(Eventual Consistency) 보장:
 *    - 비동기 메세지 릴레이(Kafka, Outbox Relay 등)는 네트워크 재시도로 인해 At-Least-Once Delivery 특성을 갖습니다.
 *    - 수신된 이벤트를 `JournalPostingPort.createDraftEntry()`로 전달하면, 
 *      내부 어댑터에서 `lineageSourceType`과 `lineageSourceId` 중복 검사를 거쳐 멱등하게 전표가 발행됩니다.
 * ───────────────────────────────────────────────────────────────────────────────────
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JournalPostingEventListener {

    private final JournalPostingPort journalPostingPort;

    /**
     * 비동기 전표 생성 이벤트 수신 핸들러.
     *
     * @param command 전표 생성 요청 커맨드 객체
     * @return 처리 결과
     */
    @EventListener
    public JournalPostingResult handleJournalPostingEvent(JournalEntryCommand command) {
        log.info("Received Async Journal Posting Event for lineageSourceId: {}, type: {}",
                command.lineageSourceId(), command.lineageSourceType());
        
        try {
            JournalPostingResult result = journalPostingPort.createDraftEntry(command);
            log.info("Successfully processed Async Journal Posting Event: slipNo={}, id={}",
                    result.slipNo(), result.journalEntryId());
            return result;
        } catch (Exception e) {
            log.error("Failed to process Async Journal Posting Event for lineageSourceId: {}",
                    command.lineageSourceId(), e);
            throw e;
        }
    }
}
