package com.ho.account.internalaudit.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ho.account.internalaudit.core.application.port.out.AuditLogPersistencePort;
import com.ho.account.internalaudit.core.application.port.out.CommandReceiptPort;
import com.ho.account.internalaudit.core.domain.AuditLogEntry;
import com.ho.account.internalaudit.core.domain.IdempotencyConflictException;
import com.ho.account.internalaudit.core.infrastructure.persistence.adapter.AuditLogPersistenceAdapter;
import com.ho.account.internalaudit.core.infrastructure.persistence.entity.AuditLogJpaEntity;
import com.ho.account.internalaudit.core.infrastructure.persistence.repository.AuditLogRepository;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;

class AuditLogPersistenceAdapterTest {

    private AuditLogRepository repository;
    private CommandReceiptPort commandReceiptPort;
    private AuditLogPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(AuditLogRepository.class);
        commandReceiptPort = mock(CommandReceiptPort.class);
        adapter = new AuditLogPersistenceAdapter(repository, commandReceiptPort);
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
        var order = inOrder(commandReceiptPort, repository);
        order.verify(commandReceiptPort).lockKey("idem-1");
        order.verify(repository).findByIdempotencyKey("idem-1");
        order.verify(repository).save(any());
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
    void integrityFailurePropagatesWithoutReadingAnAbortedTransaction() {
        AuditLogEntry entry = AuditLogEntry.builder()
                .actor("auditor_1")
                .action("CREATE_PROCESS")
                .aggregateType("RCM_PROCESS")
                .aggregateId("proc-1")
                .idempotencyKey("concurrent-key")
                .build();

        DataIntegrityViolationException failure = new DataIntegrityViolationException("constraint failure");
        when(repository.findByIdempotencyKey("concurrent-key")).thenReturn(Optional.empty());
        when(repository.save(any())).thenThrow(failure);

        assertThatThrownBy(() -> adapter.append(entry)).isSameAs(failure);
        verify(repository).findByIdempotencyKey("concurrent-key");
        verify(commandReceiptPort).lockKey("concurrent-key");
    }

    @ParameterizedTest
    @MethodSource("changedEvents")
    void conflictingDirectEventCannotReuseAnExistingKey(AuditLogEntry changedEvent) {
        when(repository.findByIdempotencyKey("direct-key")).thenReturn(Optional.of(existingEvent()));

        assertThatThrownBy(() -> adapter.append(changedEvent)).isInstanceOf(IdempotencyConflictException.class);

        verify(commandReceiptPort).lockKey("direct-key");
        verify(repository, never()).save(any());
    }

    static Stream<AuditLogEntry> changedEvents() {
        AuditLogEntry entry = directEvent();
        return Stream.of(
                entry.toBuilder().actor("other-actor").build(),
                entry.toBuilder().action("OTHER_ACTION").build(),
                entry.toBuilder().aggregateType("OTHER_TYPE").build(),
                entry.toBuilder().aggregateId("other-id").build(),
                entry.toBuilder().detailsJson("{\"value\":2}").build(),
                entry.toBuilder().detailsJson("{ \"value\": 1 }").build(),
                entry.toBuilder().detailsJson(null).build());
    }

    @Test
    void exactRetryPreservesFirstLineageDespiteNewTimestampAndCorrelation() {
        AuditLogJpaEntity existing = existingEvent();
        when(repository.findByIdempotencyKey("direct-key")).thenReturn(Optional.of(existing));

        AuditLogEntry result = adapter.append(directEvent().toBuilder()
                .idempotencyKey("  direct-key  ")
                .actionTimestamp(existing.getActionTimestamp().plusDays(1))
                .correlationId("retry-correlation")
                .build());

        assertThat(result.id()).isEqualTo(existing.getId());
        assertThat(result.correlationId()).isEqualTo("first-correlation");
        assertThat(result.actionTimestamp()).isEqualTo(existing.getActionTimestamp());
        verify(commandReceiptPort).lockKey("direct-key");
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void noKeyAppendAlwaysWritesWithoutReceiptLock(String key) {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AuditLogEntry saved = adapter.append(directEvent().toBuilder().idempotencyKey(key).build());

        assertThat(saved.idempotencyKey()).isNull();
        verify(repository).save(any());
        verify(repository, never()).findByIdempotencyKey(any());
        verifyNoInteractions(commandReceiptPort);
    }

    @Test
    void invalidKeyFailsBeforeLockOrRepositorySql() {
        assertThatThrownBy(() -> adapter.append(directEvent().toBuilder()
                .idempotencyKey("x".repeat(256)).build())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.findByIdempotencyKey("x".repeat(256)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.append(null)).isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(commandReceiptPort, repository);
    }

    private static AuditLogEntry directEvent() {
        return AuditLogEntry.builder().actor("auditor_1").action("CREATE_PROCESS")
                .aggregateType("RCM_PROCESS").aggregateId("proc-1").idempotencyKey("direct-key")
                .detailsJson("{\"value\":1}").build();
    }

    private static AuditLogJpaEntity existingEvent() {
        return AuditLogJpaEntity.builder().id(99L).actor("auditor_1").action("CREATE_PROCESS")
                .aggregateType("RCM_PROCESS").aggregateId("proc-1").idempotencyKey("direct-key")
                .detailsJson("{\"value\":1}").correlationId("first-correlation")
                .actionTimestamp(LocalDateTime.of(2026, 1, 1, 12, 0)).build();
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
