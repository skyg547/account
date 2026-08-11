package com.ho.account.contracts.outbox;

import com.ho.account.contracts.journal.JournalEntryCommand;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * [Transactional Outbox 패턴 - 전표 전용 Outbox 이벤트 (JournalOutboxEvent)]
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 전표 기록(Journal POST) 요청을 담는 전용 Outbox 이벤트 객체입니다.
 * 
 * 대출(Loan)이나 예금(Deposit) 모듈에서 거래 발생 시, 직접 외부 전표 서비스(Journal Ledger)의 API를 
 * 동기 호출하지 않고, 전표 요청 명령어 {@link JournalEntryCommand}를 담아 `JournalOutboxEvent` 형태로 
 * 로컬 DB 트랜잭션과 함께 Outbox 테이블에 원자적으로 저장합니다.
 * 
 * 이후 비동기 Outbox 릴레이(Relay)가 이 이벤트를 읽어 {@link com.ho.account.contracts.journal.JournalPostingPort}로 
 * 전표 생성을 요청합니다.
 */
public class JournalOutboxEvent {

    private final String eventId;
    private final String sourceModule; // 예: "LOAN", "DEPOSIT"
    private final String lineageSourceType; // 예: "LOAN_DISBURSAL", "DEPOSIT_ACCOUNT"
    private final String lineageSourceId; // 예: 대출ID, 계좌번호
    private final JournalEntryCommand command;
    private OutboxStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private int retryCount;
    private String errorMessage;
    private final String idempotencyKey;

    public JournalOutboxEvent(String eventId,
                             String sourceModule,
                             String lineageSourceType,
                             String lineageSourceId,
                             JournalEntryCommand command,
                             OutboxStatus status,
                             LocalDateTime createdAt,
                             LocalDateTime publishedAt,
                             int retryCount,
                             String errorMessage,
                             String idempotencyKey) {
        this.eventId = eventId != null ? eventId : UUID.randomUUID().toString();
        this.sourceModule = Objects.requireNonNull(sourceModule, "sourceModule must not be null");
        this.lineageSourceType = lineageSourceType != null ? lineageSourceType : "UNKNOWN";
        this.lineageSourceId = lineageSourceId != null ? lineageSourceId : "UNKNOWN";
        this.command = Objects.requireNonNull(command, "command must not be null");
        this.status = status != null ? status : OutboxStatus.PENDING;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.publishedAt = publishedAt;
        this.retryCount = Math.max(0, retryCount);
        this.errorMessage = errorMessage;
        // lineageSourceType + lineageSourceId 조합 또는 커맨드의 lineage 정보를 활용하여 멱등성 키 구성
        this.idempotencyKey = idempotencyKey != null ? idempotencyKey 
                : generateIdempotencyKey(this.lineageSourceType, this.lineageSourceId, this.eventId);
    }

    public static JournalOutboxEvent createPending(String sourceModule,
                                                   String lineageSourceType,
                                                   String lineageSourceId,
                                                   JournalEntryCommand command) {
        String idempotencyKey = lineageSourceType + ":" + lineageSourceId + ":" + UUID.randomUUID().toString().substring(0, 8);
        return new JournalOutboxEvent(
                UUID.randomUUID().toString(),
                sourceModule,
                lineageSourceType,
                lineageSourceId,
                command,
                OutboxStatus.PENDING,
                LocalDateTime.now(),
                null,
                0,
                null,
                idempotencyKey
        );
    }

    public static JournalOutboxEvent createPending(String sourceModule,
                                                   String lineageSourceType,
                                                   String lineageSourceId,
                                                   JournalEntryCommand command,
                                                   String idempotencyKey) {
        return new JournalOutboxEvent(
                UUID.randomUUID().toString(),
                sourceModule,
                lineageSourceType,
                lineageSourceId,
                command,
                OutboxStatus.PENDING,
                LocalDateTime.now(),
                null,
                0,
                null,
                idempotencyKey
        );
    }

    private static String generateIdempotencyKey(String lineageSourceType, String lineageSourceId, String fallbackId) {
        if (lineageSourceType != null && lineageSourceId != null) {
            return lineageSourceType + ":" + lineageSourceId;
        }
        return "JOURNAL_OUTBOX:" + fallbackId;
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

    public String getSourceModule() {
        return sourceModule;
    }

    public String getLineageSourceType() {
        return lineageSourceType;
    }

    public String getLineageSourceId() {
        return lineageSourceId;
    }

    public JournalEntryCommand getCommand() {
        return command;
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
        JournalOutboxEvent that = (JournalOutboxEvent) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return "JournalOutboxEvent{" +
                "eventId='" + eventId + '\'' +
                ", sourceModule='" + sourceModule + '\'' +
                ", lineageSourceType='" + lineageSourceType + '\'' +
                ", lineageSourceId='" + lineageSourceId + '\'' +
                ", status=" + status +
                ", retryCount=" + retryCount +
                ", idempotencyKey='" + idempotencyKey + '\'' +
                '}';
    }
}
