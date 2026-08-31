package com.ho.account.deposit.infrastructure.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.outbox.JournalOutboxEvent;
import com.ho.account.contracts.outbox.OutboxEvent;
import com.ho.account.contracts.outbox.OutboxPort;
import com.ho.account.contracts.outbox.OutboxStatus;
import com.ho.account.deposit.domain.DepositOutboxEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * [헥사고날 아키텍처 - 예금 트랜잭셔널 아웃박스 영속성 어댑터 (DepositTransactionalOutboxAdapter)]
 *
 * 🐣 [초보자를 위한 설명]
 * OutboxPort의 지속성(Durable JPA) 구현체로, 로컬 DB의 deposit_outbox 테이블에
 * 전표 이벤트 및 일반 이벤트를 안전하게 영속화하고 조회 및 상태를 갱신합니다.
 */
@Component
public class DepositTransactionalOutboxAdapter implements OutboxPort {

    private static final Logger log = Logger.getLogger(DepositTransactionalOutboxAdapter.class.getName());

    private final SpringDataDepositOutboxRepository repository;
    private final ObjectMapper objectMapper;

    @Autowired
    public DepositTransactionalOutboxAdapter(SpringDataDepositOutboxRepository repository,
                                            @Autowired(required = false) ObjectMapper objectMapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        if (objectMapper != null) {
            this.objectMapper = objectMapper;
        } else {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            this.objectMapper = mapper;
        }
    }

    @Override
    @Transactional
    public JournalOutboxEvent saveJournalEvent(JournalOutboxEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("JournalOutboxEvent must not be null");
        }

        Optional<DepositOutboxEntity> existing = repository.findByIdempotencyKey(event.getIdempotencyKey());
        if (existing.isPresent()) {
            log.info(() -> String.format("[DepositTransactionalOutboxAdapter] Outbox event already exists for key %s. Returning existing.",
                    event.getIdempotencyKey()));
            return toJournalOutboxEvent(existing.get());
        }

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(event.getCommand());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize JournalEntryCommand to JSON payload", e);
        }

        DepositOutboxEntity entity = new DepositOutboxEntity();
        entity.setEventId(event.getEventId());
        entity.setSourceModule(event.getSourceModule());
        entity.setLineageSourceType(event.getLineageSourceType());
        entity.setLineageSourceId(event.getLineageSourceId());
        entity.setEventType("JOURNAL_ENTRY");
        entity.setPayload(payloadJson);
        entity.setStatus(event.getStatus() != null ? event.getStatus() : OutboxStatus.PENDING);
        entity.setCreatedAt(event.getCreatedAt() != null ? event.getCreatedAt() : LocalDateTime.now());
        entity.setPublishedAt(event.getPublishedAt());
        entity.setRetryCount(event.getRetryCount());
        entity.setErrorMessage(event.getErrorMessage());
        entity.setIdempotencyKey(event.getIdempotencyKey());

        DepositOutboxEntity saved = repository.save(entity);
        return toJournalOutboxEvent(saved);
    }

    @Override
    @Transactional
    public OutboxEvent save(OutboxEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("OutboxEvent must not be null");
        }

        Optional<DepositOutboxEntity> existing = repository.findByIdempotencyKey(event.getIdempotencyKey());
        if (existing.isPresent()) {
            return toGeneralOutboxEvent(existing.get());
        }

        DepositOutboxEntity entity = new DepositOutboxEntity();
        entity.setEventId(event.getEventId());
        entity.setSourceModule(event.getAggregateType());
        entity.setLineageSourceType(event.getEventType());
        entity.setLineageSourceId(event.getAggregateId());
        entity.setEventType(event.getEventType());
        entity.setPayload(event.getPayload());
        entity.setStatus(event.getStatus() != null ? event.getStatus() : OutboxStatus.PENDING);
        entity.setCreatedAt(event.getCreatedAt() != null ? event.getCreatedAt() : LocalDateTime.now());
        entity.setPublishedAt(event.getPublishedAt());
        entity.setRetryCount(event.getRetryCount());
        entity.setErrorMessage(event.getErrorMessage());
        entity.setIdempotencyKey(event.getIdempotencyKey());

        DepositOutboxEntity saved = repository.save(entity);
        return toGeneralOutboxEvent(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JournalOutboxEvent> findPendingJournalEvents(int limit) {
        Pageable pageable = PageRequest.of(0, limit > 0 ? limit : 100);
        List<DepositOutboxEntity> entities = repository.findByStatusInOrderByCreatedAtAsc(
                List.of(OutboxStatus.PENDING),
                pageable
        );
        return entities.stream()
                .map(this::toJournalOutboxEvent)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> findPendingEvents(int limit) {
        Pageable pageable = PageRequest.of(0, limit > 0 ? limit : 100);
        List<DepositOutboxEntity> entities = repository.findByStatusInOrderByCreatedAtAsc(
                List.of(OutboxStatus.PENDING),
                pageable
        );
        return entities.stream()
                .map(this::toGeneralOutboxEvent)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JournalOutboxEvent> findJournalEventById(String eventId) {
        if (eventId == null) return Optional.empty();
        return repository.findByEventId(eventId)
                .map(this::toJournalOutboxEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JournalOutboxEvent> findJournalEventByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) return Optional.empty();
        return repository.findByIdempotencyKey(idempotencyKey)
                .map(this::toJournalOutboxEvent);
    }

    @Override
    @Transactional
    public void markJournalEventAsPublished(String eventId, LocalDateTime publishedAt) {
        DepositOutboxEntity entity = repository.findByEventId(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Deposit outbox event not found for eventId: " + eventId));

        if (entity.getStatus() == OutboxStatus.PUBLISHED) {
            log.fine(() -> String.format("[DepositTransactionalOutboxAdapter] Outbox event %s is already PUBLISHED. Skipping.", eventId));
            return;
        }

        entity.setStatus(OutboxStatus.PUBLISHED);
        entity.setPublishedAt(publishedAt != null ? publishedAt : LocalDateTime.now());
        entity.setErrorMessage(null);
        repository.save(entity);
    }

    @Override
    @Transactional
    public void markJournalEventAsFailed(String eventId, String errorMessage) {
        DepositOutboxEntity entity = repository.findByEventId(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Deposit outbox event not found for eventId: " + eventId));

        if (entity.getStatus() == OutboxStatus.PUBLISHED) {
            log.warning(() -> String.format("[DepositTransactionalOutboxAdapter] Outbox event %s is already PUBLISHED. Ignoring failure.", eventId));
            return;
        }

        entity.setRetryCount(entity.getRetryCount() + 1);
        entity.setErrorMessage(errorMessage);
        if (entity.getRetryCount() >= 5) {
            entity.setStatus(OutboxStatus.FAILED);
        }
        repository.save(entity);
    }

    private JournalOutboxEvent toJournalOutboxEvent(DepositOutboxEntity entity) {
        try {
            JournalEntryCommand command = objectMapper.readValue(entity.getPayload(), JournalEntryCommand.class);
            return new JournalOutboxEvent(
                    entity.getEventId(),
                    entity.getSourceModule(),
                    entity.getLineageSourceType(),
                    entity.getLineageSourceId(),
                    command,
                    entity.getStatus(),
                    entity.getCreatedAt(),
                    entity.getPublishedAt(),
                    entity.getRetryCount(),
                    entity.getErrorMessage(),
                    entity.getIdempotencyKey()
            );
        } catch (JsonProcessingException e) {
            log.severe(() -> String.format("Failed to deserialize JournalEntryCommand for outbox event %s: %s",
                    entity.getEventId(), e.getMessage()));
            return null;
        }
    }

    private OutboxEvent toGeneralOutboxEvent(DepositOutboxEntity entity) {
        return new OutboxEvent(
                entity.getEventId(),
                entity.getSourceModule(),
                entity.getLineageSourceId(),
                entity.getLineageSourceType() != null ? entity.getLineageSourceType() : entity.getEventType(),
                entity.getPayload(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getPublishedAt(),
                entity.getRetryCount(),
                entity.getErrorMessage(),
                entity.getIdempotencyKey()
        );
    }
}
