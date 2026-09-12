package com.ho.account.internalaudit.core.infrastructure.persistence.adapter;

import com.ho.account.internalaudit.core.application.port.out.AuditLogPersistencePort;
import com.ho.account.internalaudit.core.application.port.out.CommandReceiptPort;
import com.ho.account.internalaudit.core.domain.AuditLogEntry;
import com.ho.account.internalaudit.core.domain.IdempotencyConflictException;
import com.ho.account.internalaudit.core.infrastructure.persistence.entity.AuditLogJpaEntity;
import com.ho.account.internalaudit.core.infrastructure.persistence.repository.AuditLogRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
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
    private final CommandReceiptPort commandReceiptPort;

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
            if (idempotencyKey.length() > 255) {
                throw new IllegalArgumentException("Idempotency key must contain 1 to 255 characters");
            }
            // Direct append and command execution use the same transaction-held lock,
            // including first use of a key when neither receipt nor audit row exists.
            commandReceiptPort.lockKey(idempotencyKey);
            Optional<AuditLogJpaEntity> existing = repository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                if (!isSameEvent(existing.get(), entry)) {
                    throw new IdempotencyConflictException();
                }
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

        return toDomain(repository.save(entity));
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
        if (idempotencyKey.trim().length() > 255) {
            throw new IllegalArgumentException("Idempotency key must contain 1 to 255 characters");
        }
        return repository.findByIdempotencyKey(idempotencyKey.trim()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogEntry> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private boolean isSameEvent(AuditLogJpaEntity existing, AuditLogEntry entry) {
        // Retry transport lineage is deliberately excluded; the first event's
        // timestamp and correlation ID remain immutable when its result is reused.
        return Objects.equals(existing.getActor(), entry.actor())
                && Objects.equals(existing.getAction(), entry.action())
                && Objects.equals(existing.getAggregateType(), entry.aggregateType())
                && Objects.equals(existing.getAggregateId(), entry.aggregateId())
                && Objects.equals(existing.getDetailsJson(), entry.detailsJson());
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
