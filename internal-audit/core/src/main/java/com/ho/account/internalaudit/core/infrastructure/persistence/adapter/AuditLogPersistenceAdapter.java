package com.ho.account.internalaudit.core.infrastructure.persistence.adapter;

import com.ho.account.internalaudit.core.application.port.out.AuditLogPersistencePort;
import com.ho.account.internalaudit.core.domain.AuditLogEntry;
import com.ho.account.internalaudit.core.infrastructure.persistence.entity.AuditLogJpaEntity;
import com.ho.account.internalaudit.core.infrastructure.persistence.repository.AuditLogRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persistence adapter implementing AuditLogPersistencePort.
 * Strictly append-only: does NOT provide any update or delete capabilities.
 * Enforces idempotency to prevent duplicate audit records on retry.
 */
@Component
@RequiredArgsConstructor
public class AuditLogPersistenceAdapter implements AuditLogPersistencePort {

    private final AuditLogRepository repository;

    @Override
    @Transactional
    public AuditLogEntry append(AuditLogEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("AuditLogEntry must not be null");
        }

        String idempotencyKey = entry.idempotencyKey() != null && !entry.idempotencyKey().isBlank()
                ? entry.idempotencyKey().trim()
                : null;

        if (idempotencyKey != null) {
            Optional<AuditLogJpaEntity> existing = repository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return toDomain(existing.get());
            }
        }

        AuditLogJpaEntity entity = AuditLogJpaEntity.builder()
                .actor(entry.actor())
                .action(entry.action())
                .aggregateType(entry.aggregateType())
                .aggregateId(entry.aggregateId())
                .actionTimestamp(entry.actionTimestamp() != null ? entry.actionTimestamp() : LocalDateTime.now())
                .correlationId(entry.correlationId())
                .idempotencyKey(idempotencyKey)
                .detailsJson(entry.detailsJson())
                .build();

        try {
            AuditLogJpaEntity saved = repository.save(entity);
            return toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            if (idempotencyKey != null) {
                return repository.findByIdempotencyKey(idempotencyKey)
                        .map(this::toDomain)
                        .orElseThrow(() -> ex);
            }
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogEntry> findByAggregate(String aggregateType, String aggregateId) {
        return repository.findByAggregateTypeAndAggregateIdOrderByActionTimestampAsc(aggregateType, aggregateId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogEntry> findByCorrelationId(String correlationId) {
        return repository.findByCorrelationIdOrderByActionTimestampAsc(correlationId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuditLogEntry> findByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return repository.findByIdempotencyKey(idempotencyKey.trim()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogEntry> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private AuditLogEntry toDomain(AuditLogJpaEntity entity) {
        return AuditLogEntry.builder()
                .id(entity.getId())
                .actor(entity.getActor())
                .action(entity.getAction())
                .aggregateType(entity.getAggregateType())
                .aggregateId(entity.getAggregateId())
                .actionTimestamp(entity.getActionTimestamp())
                .correlationId(entity.getCorrelationId())
                .idempotencyKey(entity.getIdempotencyKey())
                .detailsJson(entity.getDetailsJson())
                .build();
    }
}
