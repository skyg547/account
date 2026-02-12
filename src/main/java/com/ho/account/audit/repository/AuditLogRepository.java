package com.ho.account.audit.repository;

import com.ho.account.audit.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEventType(String eventType);

    List<AuditLog> findByUserId(String userId);

    List<AuditLog> findByTargetEntityAndTargetId(String targetEntity, String targetId);

    List<AuditLog> findByEventDateTimeBetween(LocalDateTime start, LocalDateTime end);

    List<AuditLog> findByUserIdAndEventDateTimeBetween(String userId, LocalDateTime start, LocalDateTime end);
}
