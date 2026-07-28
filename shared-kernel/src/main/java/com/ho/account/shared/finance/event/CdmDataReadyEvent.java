package com.ho.account.shared.finance.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * account-mart 적재 완료를 ECL에 알리는 기존 이벤트 계약입니다.
 *
 * <p>eventId는 같은 Kafka 이벤트의 재전달을 같은 Spring Batch JobInstance로 식별하는 멱등 키입니다.
 * JSON 역직렬화 시 생산자가 만든 ID와 발생 시각을 보존합니다.</p>
 *
 * <p>@todo account-mart/ECL 전용 통합 계약 모듈로 이동하고 schemaVersion과 producer를 명시한다.
 * 실제 분산락은 baseDate/eventId 정책을 사용하는 별도 포트와 Redis/JDBC 어댑터로 구현한다.</p>
 */
@Getter
public class CdmDataReadyEvent {

    private final String eventId;
    private final LocalDate baseDate;
    private final String traceId;
    private final LocalDateTime occurredAt;

    public CdmDataReadyEvent(LocalDate baseDate, String traceId) {
        this(UUID.randomUUID().toString(), baseDate, traceId, LocalDateTime.now());
    }

    @JsonCreator
    public CdmDataReadyEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("baseDate") LocalDate baseDate,
            @JsonProperty("traceId") String traceId,
            @JsonProperty("occurredAt") LocalDateTime occurredAt) {
        this.eventId = requireText(eventId, "eventId");
        this.baseDate = Objects.requireNonNull(baseDate, "baseDate must not be null");
        this.traceId = requireText(traceId, "traceId");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}