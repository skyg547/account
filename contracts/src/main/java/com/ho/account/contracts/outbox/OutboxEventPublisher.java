package com.ho.account.contracts.outbox;

/**
 * [헥사고날 아키텍처 - 아웃바운드/릴레이 포트 (OutboxEventPublisher)]
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 인터페이스는 로컬 DB Outbox 저장소에 기록된 이벤트를 읽어서 
 * 실제 외부 서비스(전표 원장, 메시지 브로커 등)로 이벤트를 발행(Relay/Publish)하는 역할을 선언합니다.
 * 
 * **동작 방식:**
 * 1. `publishPendingJournalEvents()`: PENDING 상태인 전표 Outbox 이벤트를 스캔하여 
 *    {@link com.ho.account.contracts.journal.JournalPostingPort} 또는 비동기 MQ에 전달하고,
 *    성공 시 해당 이벤트를 PUBLISHED 상태로 변경합니다.
 * 2. `publish(JournalOutboxEvent event)`: 단건 Outbox 이벤트를 즉시/비동기로 발행 처리합니다.
 * 
 * **최종 정합성 (Eventual Consistency) 보장:**
 * 로컬 비즈니스 DB 커밋이 완료된 후, 이 릴레이가 비동기로 이벤트를 발행함으로써 
 * 원자성 저장과 외부 전달의 분리를 이루어 Dual Write 문제 없이 최종 정합성을 달성합니다.
 */
public interface OutboxEventPublisher {

    /**
     * 발행 대기 중인 PENDING 전표 Outbox 이벤트를 스캔하여 외부로 릴레이(발행)합니다.
     * 
     * @return 성공적으로 발행된 이벤트 개수
     */
    int publishPendingJournalEvents();

    /**
     * 특정 전표 Outbox 이벤트를 외부로 발행 처리합니다.
     * 
     * @param event 발행할 전표 Outbox 이벤트
     * @return 발행 성공 여부
     */
    boolean publish(JournalOutboxEvent event);
}
