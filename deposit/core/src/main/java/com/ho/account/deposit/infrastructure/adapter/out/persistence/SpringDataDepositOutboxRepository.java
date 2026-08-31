package com.ho.account.deposit.infrastructure.adapter.out.persistence;

import com.ho.account.contracts.outbox.OutboxStatus;
import com.ho.account.deposit.domain.DepositOutboxEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * [Spring Data JPA Repository - 예금 트랜잭셔널 아웃박스]
 *
 * deposit_outbox 테이블과 매핑되는 Spring Data JPA 인터페이스입니다.
 */
public interface SpringDataDepositOutboxRepository extends JpaRepository<DepositOutboxEntity, Long> {

    Optional<DepositOutboxEntity> findByEventId(String eventId);

    Optional<DepositOutboxEntity> findByIdempotencyKey(String idempotencyKey);

    List<DepositOutboxEntity> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);

    List<DepositOutboxEntity> findByStatusInOrderByCreatedAtAsc(List<OutboxStatus> statuses, Pageable pageable);

    @Query("SELECT o FROM DepositOutboxEntity o WHERE o.status = :status ORDER BY o.createdAt ASC")
    List<DepositOutboxEntity> findPendingEventsWithLimit(@Param("status") OutboxStatus status, Pageable pageable);

    long countByStatus(OutboxStatus status);
}
