package com.ho.account.deposit.domain;

import com.ho.account.contracts.outbox.OutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * [DDD 영속성 엔티티 - 예금 트랜잭셔널 아웃박스 (DepositOutboxEntity)]
 *
 * 🐣 [초보자를 위한 설명 및 아키텍처 배경]
 * Transactional Outbox 패턴을 지원하기 위해 `deposit_outbox` 테이블과 매핑되는 JPA 엔티티입니다.
 * 예금 계좌 개설 등 로컬 DB 트랜잭션 수반 시 비즈니스 데이터와 전표 이벤트(JournalOutboxEvent)를
 * 동일한 DB 트랜잭션 내에서 원자적(Atomically)으로 저장합니다.
 *
 * 1. eventId: 이벤트 고유 식별자 (UUID)
 * 2. idempotencyKey: 비즈니스 멱등성 키 (중복 발행 및 저장 방지)
 * 3. status: PENDING(대기), PUBLISHED(발행 완료), FAILED(실패)
 * 4. version: @Version 기반 낙관적 잠금 (동시성 제어 및 중복 릴레이 처리 방지)
 */
@Entity
@Table(name = "deposit_outbox")
@Getter
@Setter
@NoArgsConstructor
public class DepositOutboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(name = "source_module", nullable = false, length = 50)
    private String sourceModule;

    @Column(name = "lineage_source_type", nullable = false, length = 50)
    private String lineageSourceType;

    @Column(name = "lineage_source_id", nullable = false, length = 100)
    private String lineageSourceId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType = "JOURNAL_ENTRY";

    @Lob
    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 150)
    private String idempotencyKey;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = OutboxStatus.PENDING;
        }
        if (this.eventType == null) {
            this.eventType = "JOURNAL_ENTRY";
        }
        if (this.version == null) {
            this.version = 0L;
        }
    }
}
