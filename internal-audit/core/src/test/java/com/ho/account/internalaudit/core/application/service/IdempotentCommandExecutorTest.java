package com.ho.account.internalaudit.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.ho.account.internalaudit.core.application.AuditActorContext;
import com.ho.account.internalaudit.core.application.port.out.AuditLogPersistencePort;
import com.ho.account.internalaudit.core.application.port.out.CommandReceiptPort;
import com.ho.account.internalaudit.core.domain.AuditLogEntry;
import com.ho.account.internalaudit.core.domain.CommandReceipt;
import com.ho.account.internalaudit.core.domain.IdempotencyConflictException;
import com.ho.account.internalaudit.core.domain.rcm.RcmProcess;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class IdempotentCommandExecutorTest {

    private final CommandReceiptPort receiptPort = mock(CommandReceiptPort.class);
    private final AuditLogPersistencePort audits = mock(AuditLogPersistencePort.class);
    private final Map<String, CommandReceipt> receipts = new HashMap<>();
    private final Map<String, AuditLogEntry> storedAudits = new HashMap<>();
    private final RcmProcess original = new RcmProcess("p", "first response", null, "owner");
    @SuppressWarnings("unchecked")
    private final Supplier<RcmProcess> business = mock(Supplier.class);
    private IdempotentCommandExecutor executor;

    @BeforeEach
    void setUp() {
        when(receiptPort.findByKey(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(receipts.get(inv.getArgument(0))));
        org.mockito.Mockito.doAnswer(inv -> {
            String key = inv.getArgument(0);
            receipts.put(key, new CommandReceipt(key, inv.getArgument(1), inv.getArgument(2), 0, null));
            return null;
        }).when(receiptPort).reserve(anyString(), anyInt(), anyString());
        org.mockito.Mockito.doAnswer(inv -> {
            String key = inv.getArgument(0);
            CommandReceipt receipt = receipts.get(key);
            receipts.put(key, new CommandReceipt(key, receipt.fingerprintVersion(), receipt.fingerprint(),
                    inv.getArgument(1), inv.getArgument(2)));
            return null;
        }).when(receiptPort).complete(anyString(), anyInt(), anyString());
        when(audits.findByIdempotencyKey(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(storedAudits.get(inv.getArgument(0))));
        when(audits.append(any())).thenAnswer(inv -> {
            AuditLogEntry entry = inv.getArgument(0);
            if (entry.idempotencyKey() != null) {
                storedAudits.put(entry.idempotencyKey(), entry);
            }
            return entry;
        });
        when(business.get()).thenReturn(original);
        executor = new IdempotentCommandExecutor(receiptPort, audits, new ObjectMapper());
        AuditActorContext.setIdempotencyKey("key");
    }

    @AfterEach
    void clearContext() {
        AuditActorContext.clear();
    }

    @Test
    void locksBeforeBothLookupsAndAtomicallyOrdersReserveBusinessAuditAndComplete() {
        assertThat(execute(original)).isEqualTo(original);
        InOrder order = inOrder(receiptPort, audits, business);
        order.verify(receiptPort).lockKey("key");
        order.verify(receiptPort).findByKey("key");
        order.verify(audits).findByIdempotencyKey("key");
        order.verify(receiptPort).reserve(org.mockito.ArgumentMatchers.eq("key"),
                org.mockito.ArgumentMatchers.eq(1), anyString());
        order.verify(business).get();
        order.verify(audits).append(any());
        order.verify(receiptPort).complete(org.mockito.ArgumentMatchers.eq("key"),
                org.mockito.ArgumentMatchers.eq(1), anyString());
        order.verifyNoMoreInteractions();
    }

    @Test
    void trimmedKeyReplaysFirstSavedSnapshotAndKeepsOriginalAuditLineage() {
        AuditActorContext.setIdempotencyKey(" key ");
        AuditActorContext.setCorrelationId("first-trace");
        RcmProcess command = new RcmProcess("p", "requested", null, "owner");
        assertThat(execute(command)).isEqualTo(original);
        AuditActorContext.setCorrelationId("retry-trace");
        AuditActorContext.setIdempotencyKey("key");
        assertThat(execute(command)).isEqualTo(original);

        verify(business).get();
        ArgumentCaptor<AuditLogEntry> audit = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(audits).append(audit.capture());
        assertThat(audit.getValue().correlationId()).isEqualTo("first-trace");
        assertThat(audit.getValue().detailsJson()).contains("first response").doesNotContain("requested");
        verify(receiptPort).reserve(anyString(), anyInt(), anyString());
        verify(receiptPort).complete(anyString(), anyInt(), anyString());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  \t "})
    void absentKeyExecutesAndAuditsEveryInvocationWithoutReceipts(String key) {
        AuditActorContext.setIdempotencyKey(key);
        execute(original);
        execute(original);

        verify(business, times(2)).get();
        verify(audits, times(2)).append(any());
        verifyNoInteractions(receiptPort);
    }

    @ParameterizedTest
    @ValueSource(strings = {"actor", "action", "aggregateType", "aggregateId", "path", "payload"})
    void changedIdentityComponentConflictsInTheGlobalNamespaceBeforeBusiness(String component) {
        execute(original);
        clearInvocations(receiptPort, audits, business);
        assertThatThrownBy(() -> executor.execute(
                component.equals("actor") ? "other" : "actor",
                component.equals("action") ? "OTHER_ACTION" : "CREATE_PROCESS",
                component.equals("aggregateType") ? "OTHER_TYPE" : "RCM_PROCESS",
                component.equals("aggregateId") ? "other" : "p",
                component.equals("path") ? "other" : null,
                component.equals("payload") ? new RcmProcess("p", "changed", null, "owner") : original,
                RcmProcess.class, business))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessage("Idempotency key cannot be reused for this command.");
        verifyNoInteractions(audits, business);
        verify(receiptPort, never()).reserve(anyString(), anyInt(), anyString());
        verify(receiptPort, never()).complete(anyString(), anyInt(), anyString());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not JSON", "{\"processId\":\"p\"}"})
    void legacyAuditCannotProveCommandEvenWhenItsDetailsMatch(String details) {
        when(audits.findByIdempotencyKey("key")).thenReturn(Optional.of(AuditLogEntry.builder()
                .actor("actor").action("CREATE_PROCESS").aggregateType("RCM_PROCESS").aggregateId("p")
                .idempotencyKey("key").detailsJson(details).build()));
        assertThatThrownBy(() -> execute(original)).isInstanceOf(IdempotencyConflictException.class);
        verifyNoInteractions(business);
        verify(receiptPort, never()).reserve(anyString(), anyInt(), anyString());
        verify(audits, never()).append(any());
    }

    @Test
    void completeReceiptRequiresMatchingImmutableAuditWithoutAnotherAppend() {
        execute(original);
        clearInvocations(audits);
        execute(original);
        verify(audits).findByIdempotencyKey("key");
        verify(audits, never()).append(any());
    }

    @Test
    void canonicalIdentitySortsNestedMapsPreservesArraysAndUsesExplicitNullsDespiteHttpMapper() {
        ObjectMapper httpMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        executor = new IdempotentCommandExecutor(receiptPort, audits, httpMapper);
        Map<String, Object> nestedFirst = new LinkedHashMap<>();
        nestedFirst.put("b", 2);
        nestedFirst.put("a", 1);
        Map<String, Object> nestedReordered = new LinkedHashMap<>();
        nestedReordered.put("a", 1);
        nestedReordered.put("b", 2);
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("nullable", null);
        first.put("nested", nestedFirst);
        first.put("array", List.of("b", "a"));
        Map<String, Object> reordered = new LinkedHashMap<>();
        reordered.put("array", List.of("b", "a"));
        reordered.put("nested", nestedReordered);
        reordered.put("nullable", null);

        execute(first);
        execute(reordered);
        verify(business).get();
        assertThat(receipts.get("key").snapshotJson()).contains("\"description\":null");
        assertThat(httpMapper.getSerializationConfig().getDefaultPropertyInclusion().getValueInclusion())
                .isEqualTo(JsonInclude.Include.NON_NULL);
        reordered.put("array", List.of("a", "b"));
        assertThatThrownBy(() -> execute(reordered)).isInstanceOf(IdempotencyConflictException.class);
        reordered.put("array", List.of("b", "a"));
        reordered.remove("nullable");
        assertThatThrownBy(() -> execute(reordered)).isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void fingerprintV1HasStableSha256Format() {
        execute(original);
        assertThat(receipts.get("key").fingerprintVersion()).isEqualTo(1);
        assertThat(receipts.get("key").fingerprint())
                .isEqualTo("db27c41fe19a846bbedf5095eed3b50e20d3ba8781254ba1226e0fcab0840de3");
    }

    @Test
    void v1FingerprintAndSnapshotRemainStableWhenHttpFormattingChanges() {
        execute(original);
        CommandReceipt first = receipts.get("key");
        ObjectMapper httpMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .enable(SerializationFeature.INDENT_OUTPUT, SerializationFeature.WRAP_ROOT_VALUE,
                        SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                .enable(DeserializationFeature.UNWRAP_ROOT_VALUE,
                        DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES);
        executor = new IdempotentCommandExecutor(receiptPort, audits, httpMapper);
        AuditActorContext.setIdempotencyKey("second");
        assertThat(execute(original)).isEqualTo(original);
        CommandReceipt second = receipts.get("second");

        assertThat(second.fingerprint()).isEqualTo(first.fingerprint());
        assertThat(second.snapshotJson()).isEqualTo(first.snapshotJson());
        AuditActorContext.setIdempotencyKey("key");
        assertThat(execute(original)).isEqualTo(original);
        assertThat(httpMapper.isEnabled(SerializationFeature.INDENT_OUTPUT)).isTrue();
        assertThat(httpMapper.getPropertyNamingStrategy()).isEqualTo(PropertyNamingStrategies.SNAKE_CASE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"key", "fingerprintVersion", "fingerprintNull", "fingerprintMalformed",
            "snapshotVersion", "incomplete", "snapshotNull", "snapshotBlank"})
    void unsupportedOrIncompleteReceiptFailsSafelyWithoutReexecution(String corruption) {
        execute(original);
        CommandReceipt valid = receipts.get("key");
        receipts.put("key", new CommandReceipt(corruption.equals("key") ? "different" : valid.idempotencyKey(),
                corruption.equals("fingerprintVersion") ? 2 : valid.fingerprintVersion(),
                corruption.equals("fingerprintNull") ? null
                        : corruption.equals("fingerprintMalformed") ? "invalid" : valid.fingerprint(),
                corruption.equals("snapshotVersion") ? 2 : corruption.equals("incomplete") ? 0 : 1,
                corruption.equals("snapshotNull") ? null : corruption.equals("snapshotBlank") ? " " : valid.snapshotJson()));
        clearInvocations(receiptPort, audits, business);

        assertThatThrownBy(() -> execute(original)).isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to process the command.");
        verifyNoInteractions(audits, business);
        verify(receiptPort, never()).reserve(anyString(), anyInt(), anyString());
        verify(receiptPort, never()).complete(anyString(), anyInt(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not-json", "null", "[]", "{}",
            "{\"processId\":\"p\",\"processName\":\"first response\",\"ownerId\":\"owner\"}",
            "{\"processId\":\"p\",\"processName\":4,\"description\":null,\"ownerId\":\"owner\"}",
            "{\"processId\":\"p\",\"processName\":\"first response\",\"description\":null,\"ownerId\":\"owner\",\"extra\":null}",
            "{\"processId\":\"p\",\"processId\":\"other\",\"processName\":\"first response\",\"description\":null,\"ownerId\":\"owner\"}",
            "{\"processId\":\"p\",\"processName\":\"first response\",\"description\":null,\"ownerId\":\"owner\"} {}"
    })
    void corruptSnapshotFailsWithoutFallbackOrRewrite(String snapshot) {
        execute(original);
        CommandReceipt valid = receipts.get("key");
        receipts.put("key", new CommandReceipt("key", 1, valid.fingerprint(), 1, snapshot));
        storedAudits.put("key", storedAudits.get("key").toBuilder().detailsJson(snapshot).build());
        clearInvocations(receiptPort, audits, business);

        assertThatThrownBy(() -> execute(original)).isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to process the command.");
        verifyNoInteractions(business);
        verify(audits, never()).append(any());
        verify(receiptPort, never()).reserve(anyString(), anyInt(), anyString());
        verify(receiptPort, never()).complete(anyString(), anyInt(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "actor", "action", "type", "id", "key", "snapshot"})
    void replayRequiresAnAuditConsistentWithTheReceiptAndOriginalIdentity(String corruption) {
        execute(original);
        AuditLogEntry audit = storedAudits.get("key");
        if (corruption.equals("missing")) {
            storedAudits.clear();
        } else {
            storedAudits.put("key", switch (corruption) {
                case "actor" -> audit.toBuilder().actor("other").build();
                case "action" -> audit.toBuilder().action("OTHER").build();
                case "type" -> audit.toBuilder().aggregateType("OTHER").build();
                case "id" -> audit.toBuilder().aggregateId("other").build();
                case "key" -> audit.toBuilder().idempotencyKey("other").build();
                default -> audit.toBuilder().detailsJson("{}").build();
            });
        }
        clearInvocations(receiptPort, audits, business);

        assertThatThrownBy(() -> execute(original)).isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to process the command.");
        verifyNoInteractions(business);
        verify(audits, never()).append(any());
        verify(receiptPort, never()).reserve(anyString(), anyInt(), anyString());
        verify(receiptPort, never()).complete(anyString(), anyInt(), anyString());
    }

    @Test
    void validJsonWithChangedResultCannotBeReplayedAgainstTheOriginalAudit() {
        execute(original);
        CommandReceipt valid = receipts.get("key");
        receipts.put("key", new CommandReceipt("key", 1, valid.fingerprint(), 1,
                valid.snapshotJson().replace("\"processId\":\"p\"", "\"processId\":null")));
        clearInvocations(receiptPort, audits, business);

        assertThatThrownBy(() -> execute(original)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(business);
        verify(audits, never()).append(any());
        verify(receiptPort, never()).complete(anyString(), anyInt(), anyString());
    }

    @Test
    void businessFailurePropagatesAndPreventsAuditAndReceiptCompletion() {
        when(business.get()).thenThrow(new IllegalStateException("synthetic write failure"));
        assertThatThrownBy(() -> execute(original)).hasMessage("synthetic write failure");
        verify(audits, never()).append(any());
        verify(receiptPort, never()).complete(anyString(), anyInt(), anyString());
    }

    @Test
    void snapshotSerializationFailureAfterBusinessPreventsAuditAndCompletion() {
        RcmProcess brokenResult = org.mockito.Mockito.spy(original);
        when(brokenResult.processName()).thenThrow(new IllegalStateException("synthetic snapshot failure"));
        when(business.get()).thenReturn(brokenResult);
        assertThatThrownBy(() -> execute(original)).isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to process the command.");
        verify(business).get();
        verify(audits, never()).append(any());
        verify(receiptPort, never()).complete(anyString(), anyInt(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"actor", "action", "aggregateType", "aggregateId", "correlation",
            "key", "details", "timestamp", "null"})
    void mismatchedAuditReturnFailsBeforeReceiptCompletion(String mismatch) {
        org.mockito.Mockito.doAnswer(inv -> {
            AuditLogEntry entry = inv.getArgument(0);
            return switch (mismatch) {
                case "actor" -> entry.toBuilder().actor("other").build();
                case "action" -> entry.toBuilder().action("other").build();
                case "aggregateType" -> entry.toBuilder().aggregateType("other").build();
                case "aggregateId" -> entry.toBuilder().aggregateId("other").build();
                case "correlation" -> entry.toBuilder().correlationId("other").build();
                case "key" -> entry.toBuilder().idempotencyKey("other").build();
                case "details" -> entry.toBuilder().detailsJson("{}").build();
                case "timestamp" -> entry.toBuilder().actionTimestamp(entry.actionTimestamp().minusSeconds(1)).build();
                default -> null;
            };
        }).when(audits).append(any());
        assertThatThrownBy(() -> execute(original)).isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to process the command.");
        verify(receiptPort, never()).complete(anyString(), anyInt(), anyString());
    }

    @Test
    void overlongKeyFailsBeforeAnyPortOrBusinessInvocation() {
        AuditActorContext.setIdempotencyKey("k".repeat(256));
        assertThatThrownBy(() -> execute(original)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(receiptPort, audits, business);
    }

    private RcmProcess execute(Object command) {
        return executor.execute("actor", "CREATE_PROCESS", "RCM_PROCESS", "p", null,
                command, RcmProcess.class, business);
    }
}
