package com.ho.account.contracts.outbox;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * [Transactional Outbox 패턴 - 인메모리 포트 어댑터 (InMemoryOutboxAdapter)]
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 {@link OutboxPort}의 인메모리 구현체입니다.
 * 로컬 단독 테스트 환경이나 별도의 DB Outbox 테이블 설정 없이도 
 * Transactional Outbox 패턴의 원자적 저장 및 비동기 릴레이 흐름을 
 * 검증할 수 있도록 지원합니다.
 */
public class InMemoryOutboxAdapter implements OutboxPort {

    private final Map<String, JournalOutboxEvent> journalEvents = new ConcurrentHashMap<>();
    private final Map<String, OutboxEvent> generalEvents = new ConcurrentHashMap<>();

    @Override
    public JournalOutboxEvent saveJournalEvent(JournalOutboxEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("JournalOutboxEvent must not be null.");
        }
        journalEvents.put(event.getEventId(), event);
        return event;
    }

    @Override
    public OutboxEvent save(OutboxEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("OutboxEvent must not be null.");
        }
        generalEvents.put(event.getEventId(), event);
        return event;
    }

    @Override
    public List<JournalOutboxEvent> findPendingJournalEvents(int limit) {
        return journalEvents.values().stream()
                .filter(e -> e.getStatus() == OutboxStatus.PENDING)
                .sorted((e1, e2) -> e1.getCreatedAt().compareTo(e2.getCreatedAt()))
                .limit(limit > 0 ? limit : Integer.MAX_VALUE)
                .collect(Collectors.toList());
    }

    @Override
    public List<OutboxEvent> findPendingEvents(int limit) {
        return generalEvents.values().stream()
                .filter(e -> e.getStatus() == OutboxStatus.PENDING)
                .sorted((e1, e2) -> e1.getCreatedAt().compareTo(e2.getCreatedAt()))
                .limit(limit > 0 ? limit : Integer.MAX_VALUE)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<JournalOutboxEvent> findJournalEventById(String eventId) {
        return Optional.ofNullable(journalEvents.get(eventId));
    }

    @Override
    public Optional<JournalOutboxEvent> findJournalEventByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) return Optional.empty();
        return journalEvents.values().stream()
                .filter(e -> idempotencyKey.equals(e.getIdempotencyKey()))
                .findFirst();
    }

    @Override
    public void markJournalEventAsPublished(String eventId, LocalDateTime publishedAt) {
        JournalOutboxEvent event = journalEvents.get(eventId);
        if (event != null) {
            event.markAsPublished(publishedAt);
        }
    }

    @Override
    public void markJournalEventAsFailed(String eventId, String errorMessage) {
        JournalOutboxEvent event = journalEvents.get(eventId);
        if (event != null) {
            event.markAsFailed(errorMessage);
        }
    }

    public List<JournalOutboxEvent> getAllJournalEvents() {
        return Collections.unmodifiableList(new ArrayList<>(journalEvents.values()));
    }

    public void clear() {
        journalEvents.clear();
        generalEvents.clear();
    }
}
