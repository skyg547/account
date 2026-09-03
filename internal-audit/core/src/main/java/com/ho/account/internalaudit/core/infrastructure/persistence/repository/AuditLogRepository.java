package com.ho.account.internalaudit.core.infrastructure.persistence.repository;

import com.ho.account.internalaudit.core.infrastructure.persistence.entity.AuditLogJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogJpaEntity, Long> {

    List<AuditLogJpaEntity> findByAggregateTypeAndAggregateIdOrderByActionTimestampAsc(
            String aggregateType, String aggregateId);

    List<AuditLogJpaEntity> findByCorrelationIdOrderByActionTimestampAsc(String correlationId);

    Optional<AuditLogJpaEntity> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
