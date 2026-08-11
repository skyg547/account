package com.ho.account.contracts.outbox;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * [Transactional Outbox 패턴 - 공통 이벤트 모델 (OutboxEvent)]
 * 
 * 🐣 [초보자를 위한 설명 및 아키텍처 배경]
 * 1. **Dual Write 정합성 문제란?**
 *    마이크로서비스 아키텍처(MSA)에서는 한 트랜잭션 내에서 (1) 로컬 RDB에 데이터 저장(save)과 
 *    (2) 외부 전표 서비스 호출(Journal POST API / Kafka Publish)을 동시에 수행할 때 문제가 발생합니다.
 *    - 로컬 DB commit에는 성공했으나 네트워크 단절로 외부 전표 API 호출이 실패한 경우
 *    - 외부 전표 API 호출에는 성공했으나 로컬 DB commit 직전 예외가 발생하여 로컬 트랜잭션이 롤백된 경우
 *    이 두 경우 모두 서비스 간 데이터 불일치(Inconsistency)가 발생하며, 2-Phase Commit(2PC)은 performance와 availability를 저하시킵니다.
 * 
 * 2. **Transactional Outbox 패턴의 해결 원리:**
 *    외부 직접 호출 대신, 동일한 로컬 DB 트랜잭션 내에 `Outbox` 테이블(이벤트 저장소)을 만들어 
 *    비즈니스 엔티티와 Outbox 이벤트를 원자적(Atomic)으로 함께 저장합니다.
 *    이후 별도의 비동기 릴레이(Outbox Relay / Publisher Engine)가 PENDING 상태의 Outbox 이벤트를 읽어서 
 *    외부 서비스로 안전하게 전달(At-Least-Once Delivery)합니다.
 * 
 * 3. **최종 정합성 (Eventual Consistency) 및 멱등성 (Idempotency):**
 *    비동기 메시지 전달 과정에서 네트워크 재시도 등으로 인해 동일 이벤트가 중복 전달(At-Least-Once)될 수 있습니다.
 *    따라서 수신측에서는 `idempotencyKey` 또는 `eventId`를 검증하여 중복 처리를 방지함으로써 
 *    최종적으로 시스템 간 데이터 정합성을 달성합니다.
 */
public class OutboxEvent {

    private final String eventId;
    private final String aggregateType;
    private final String aggregateId;
    private final String eventType;
    private final String payload;
    private OutboxStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private int retryCount;
    private String errorMessage;
    private final String idempotencyKey;

    public OutboxEvent(String eventId,
                       String aggregateType,
                       String aggregateId,
                       String eventType,
                       String payload,
                       OutboxStatus status,
                       LocalDateTime createdAt,
                       LocalDateTime publishedAt,
                       int retryCount,
                       String errorMessage,
                       String idempotencyKey) {
        this.eventId = eventId != null ? eventId : UUID.randomUUID().toString();
        this.aggregateType = Objects.requireNonNull(aggregateType, "aggregateType must not be null");
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
        this.status = status != null ? status : OutboxStatus.PENDING;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.publishedAt = publishedAt;
        this.retryCount = Math.max(0, retryCount);
        this.errorMessage = errorMessage;
        this.idempotencyKey = idempotencyKey != null ? idempotencyKey : this.eventId;
    }

    public static OutboxEvent createPending(String aggregateType,
                                            String aggregateId,
                                            String eventType,
                                            String payload,
                                            String idempotencyKey) {
        return new OutboxEvent(
                UUID.randomUUID().toString(),
                aggregateType,
                aggregateId,
                eventType,
                payload,
                OutboxStatus.PENDING,
                LocalDateTime.now(),
                null,
                0,
                null,
                idempotencyKey
        );
    }

    public void markAsPublished(LocalDateTime publishedAt) {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = publishedAt != null ? publishedAt : LocalDateTime.now();
        this.errorMessage = null;
    }

    public void markAsFailed(String errorMessage) {
        this.retryCount++;
        this.errorMessage = errorMessage;
        if (this.retryCount >= 5) {
            this.status = OutboxStatus.FAILED;
        }
    }

    public String getEventId() {
        return eventId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OutboxEvent that = (OutboxEvent) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return "OutboxEvent{" +
                "eventId='" + eventId + '\'' +
                ", aggregateType='" + aggregateType + '\'' +
                ", aggregateId='" + aggregateId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", status=" + status +
                ", retryCount=" + retryCount +
                ", idempotencyKey='" + idempotencyKey + '\'' +
                '}';
    }
}
