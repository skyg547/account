package com.ho.account.contracts.outbox;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * [헥사고날 아키텍처 - 아웃바운드 포트 (OutboxPort)]
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 인터페이스는 Transactional Outbox 패턴에서 Outbox 이벤트를 DB(또는 인메모리 저장소)에 
 * 저장하고 조회 및 상태를 업데이트하는 영속성 아웃바운드 포트(Output Port)입니다.
 * 
 * **핵심 역할:**
 * 1. 로컬 트랜잭션 내에서 비즈니스 데이터(예: 대출, 예금)와 함께 Outbox 이벤트를 로컬 저장소에 원자적으로 Save.
 * 2. 비동기 릴레이 프로세서(Outbox Relay Engine)가 아직 전달되지 않은 `PENDING` 상태의 이벤트들을 조회.
 * 3. 외부 전달 완료 후 이벤트 상태를 `PUBLISHED`로 변경하거나 실패 시 `FAILED` 상태 및 에러 메시지 기록.
 */
public interface OutboxPort {

    /**
     * 전표 전용 Outbox 이벤트를 로컬 저장소에 원자적으로 저장합니다.
     */
    JournalOutboxEvent saveJournalEvent(JournalOutboxEvent event);

    /**
     * 일반 Outbox 이벤트를 로컬 저장소에 원자적으로 저장합니다.
     */
    OutboxEvent save(OutboxEvent event);

    /**
     * 발행 대기 중인 PENDING 상태의 전표 Outbox 이벤트를 조회합니다.
     * 
     * @param limit 조회할 최대 건수
     * @return 발행 대기 이벤트 리스트
     */
    List<JournalOutboxEvent> findPendingJournalEvents(int limit);

    /**
     * 발행 대기 중인 PENDING 상태의 일반 Outbox 이벤트를 조회합니다.
     * 
     * @param limit 조회할 최대 건수
     * @return 발행 대기 이벤트 리스트
     */
    List<OutboxEvent> findPendingEvents(int limit);

    /**
     * eventId로 전표 Outbox 이벤트를 단건 조회합니다.
     */
    Optional<JournalOutboxEvent> findJournalEventById(String eventId);

    /**
     * 멱등성 키(idempotencyKey)로 전표 Outbox 이벤트를 단건 조회합니다.
     */
    Optional<JournalOutboxEvent> findJournalEventByIdempotencyKey(String idempotencyKey);

    /**
     * 지정된 Outbox 이벤트를 PUBLISHED (발행 성공) 상태로 전환합니다.
     */
    void markJournalEventAsPublished(String eventId, LocalDateTime publishedAt);

    /**
     * 지정된 Outbox 이벤트를 실패 상태로 갱신하거나 재시도 횟수를 증가시킵니다.
     */
    void markJournalEventAsFailed(String eventId, String errorMessage);
}
