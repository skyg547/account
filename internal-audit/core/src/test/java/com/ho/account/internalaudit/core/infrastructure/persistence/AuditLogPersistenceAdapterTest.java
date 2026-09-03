package com.ho.account.internalaudit.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.internalaudit.core.application.port.out.AuditLogPersistencePort;
import com.ho.account.internalaudit.core.domain.AuditLogEntry;
import com.ho.account.internalaudit.core.infrastructure.persistence.adapter.AuditLogPersistenceAdapter;
import com.ho.account.internalaudit.core.infrastructure.persistence.entity.AuditLogJpaEntity;
import com.ho.account.internalaudit.core.infrastructure.persistence.repository.AuditLogRepository;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class AuditLogPersistenceAdapterTest {

    private AuditLogRepository repository;
    private AuditLogPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(AuditLogRepository.class);
        adapter = new AuditLogPersistenceAdapter(repository);
    }

    @Test
    void portEnforcesStrictAppendOnlySemanticsWithoutUpdateOrDeleteMethods() {
        Method[] methods = AuditLogPersistencePort.class.getMethods();

        for (Method method : methods) {
            String name = method.getName().toLowerCase();
            assertThat(name)
                    .doesNotContain("update")
                    .doesNotContain("delete")
                    .doesNotContain("remove")
                    .doesNotContain("modify")
                    .doesNotContain("clear")
                    .doesNotContain("patch");
        }
    }

    @Test
    void appendPersistsNewAuditLogRecord() {
        LocalDateTime now = LocalDateTime.now();
        AuditLogEntry entry = AuditLogEntry.builder()
                .actor("auditor_1")
                .action("CREATE_PROCESS")
                .aggregateType("RCM_PROCESS")
                .aggregateId("proc-1")
                .actionTimestamp(now)
                .correlationId("corr-1")
                .idempotencyKey("idem-1")
                .detailsJson("{\"desc\":\"test\"}")
                .build();

        AuditLogJpaEntity savedEntity = AuditLogJpaEntity.builder()
                .id(100L)
                .actor("auditor_1")
                .action("CREATE_PROCESS")
                .aggregateType("RCM_PROCESS")
                .aggregateId("proc-1")
                .actionTimestamp(now)
                .correlationId("corr-1")
                .idempotencyKey("idem-1")
                .detailsJson("{\"desc\":\"test\"}")
                .build();

        when(repository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(savedEntity);

        AuditLogEntry saved = adapter.append(entry);

        assertThat(saved.id()).isEqualTo(100L);
        assertThat(saved.actor()).isEqualTo("auditor_1");
        assertThat(saved.action()).isEqualTo("CREATE_PROCESS");
        assertThat(saved.aggregateType()).isEqualTo("RCM_PROCESS");
        assertThat(saved.aggregateId()).isEqualTo("proc-1");
        assertThat(saved.correlationId()).isEqualTo("corr-1");
        assertThat(saved.idempotencyKey()).isEqualTo("idem-1");
        verify(repository).save(any());
    }

    @Test
    void retryingWithSameIdempotencyKeyDoesNotDuplicateRecord() {
        AuditLogEntry entry = AuditLogEntry.builder()
                .actor("auditor_1")
                .action("CREATE_PROCESS")
                .aggregateType("RCM_PROCESS")
                .aggregateId("proc-1")
                .idempotencyKey("idem-duplicate-check")
                .build();

        AuditLogJpaEntity existingEntity = AuditLogJpaEntity.builder()
                .id(42L)
                .actor("auditor_1")
                .action("CREATE_PROCESS")
                .aggregateType("RCM_PROCESS")
                .aggregateId("proc-1")
                .actionTimestamp(LocalDateTime.now())
                .idempotencyKey("idem-duplicate-check")
                .build();

        when(repository.findByIdempotencyKey("idem-duplicate-check")).thenReturn(Optional.of(existingEntity));

        AuditLogEntry result = adapter.append(entry);

        assertThat(result.id()).isEqualTo(42L);
        verify(repository, never()).save(any());
    }

    @Test
    void concurrentConstraintViolationRecoversExistingIdempotentRecord() {
        AuditLogEntry entry = AuditLogEntry.builder()
                .actor("auditor_1")
                .action("CREATE_PROCESS")
                .aggregateType("RCM_PROCESS")
                .aggregateId("proc-1")
                .idempotencyKey("concurrent-key")
                .build();

        AuditLogJpaEntity existingEntity = AuditLogJpaEntity.builder()
                .id(99L)
                .actor("auditor_1")
                .action("CREATE_PROCESS")
                .aggregateType("RCM_PROCESS")
                .aggregateId("proc-1")
                .actionTimestamp(LocalDateTime.now())
                .idempotencyKey("concurrent-key")
                .build();

        // First check returns empty, but save throws DataIntegrityViolationException (race condition)
        when(repository.findByIdempotencyKey("concurrent-key"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingEntity));
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key uq_internal_audit_log_idempotency"));

        AuditLogEntry result = adapter.append(entry);

        assertThat(result.id()).isEqualTo(99L);
        assertThat(result.idempotencyKey()).isEqualTo("concurrent-key");
    }

    @Test
    void findByAggregateRetrievesAuditRecordsInTimestampOrder() {
        AuditLogJpaEntity entity1 = AuditLogJpaEntity.builder()
                .id(1L)
                .actor("auditor_1")
                .action("CREATE_PROCESS")
                .aggregateType("RCM_PROCESS")
                .aggregateId("proc-1")
                .actionTimestamp(LocalDateTime.now().minusMinutes(5))
                .build();
        AuditLogJpaEntity entity2 = AuditLogJpaEntity.builder()
                .id(2L)
                .actor("auditor_2")
                .action("UPDATE_PROCESS")
                .aggregateType("RCM_PROCESS")
                .aggregateId("proc-1")
                .actionTimestamp(LocalDateTime.now())
                .build();

        when(repository.findByAggregateTypeAndAggregateIdOrderByActionTimestampAsc("RCM_PROCESS", "proc-1"))
                .thenReturn(List.of(entity1, entity2));

        List<AuditLogEntry> logs = adapter.findByAggregate("RCM_PROCESS", "proc-1");

        assertThat(logs).hasSize(2);
        assertThat(logs.get(0).id()).isEqualTo(1L);
        assertThat(logs.get(1).id()).isEqualTo(2L);
    }

    @Test
    void findByCorrelationIdRetrievesAllRelatedAuditLogs() {
        AuditLogJpaEntity entity1 = AuditLogJpaEntity.builder()
                .id(10L)
                .actor("auditor_1")
                .action("CREATE_PROCESS")
                .aggregateType("RCM_PROCESS")
                .aggregateId("proc-1")
                .correlationId("tx-trace-123")
                .actionTimestamp(LocalDateTime.now().minusSeconds(10))
                .build();
        AuditLogJpaEntity entity2 = AuditLogJpaEntity.builder()
                .id(11L)
                .actor("auditor_1")
                .action("ADD_RISK")
                .aggregateType("RCM_RISK")
                .aggregateId("risk-1")
                .correlationId("tx-trace-123")
                .actionTimestamp(LocalDateTime.now())
                .build();

        when(repository.findByCorrelationIdOrderByActionTimestampAsc("tx-trace-123"))
                .thenReturn(List.of(entity1, entity2));

        List<AuditLogEntry> logs = adapter.findByCorrelationId("tx-trace-123");

        assertThat(logs).hasSize(2);
        assertThat(logs).extracting(AuditLogEntry::aggregateType).containsExactly("RCM_PROCESS", "RCM_RISK");
    }
}
