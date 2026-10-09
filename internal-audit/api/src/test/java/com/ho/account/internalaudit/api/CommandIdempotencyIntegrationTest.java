package com.ho.account.internalaudit.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ho.account.internalaudit.api.adapter.in.web.InternalAuditIdentityFilter;
import com.ho.account.internalaudit.api.adapter.in.web.InternalAuditPrincipal;
import com.ho.account.internalaudit.core.application.AuditActorContext;
import com.ho.account.internalaudit.core.application.port.out.AuditLogPersistencePort;
import com.ho.account.internalaudit.core.application.port.out.CommandReceiptPort;
import com.ho.account.internalaudit.core.application.port.out.EvaluationPersistencePort;
import com.ho.account.internalaudit.core.application.port.out.RcmPersistencePort;
import com.ho.account.internalaudit.core.domain.AuditLogEntry;
import com.ho.account.internalaudit.core.domain.evaluation.Deficiency;
import com.ho.account.internalaudit.core.domain.evaluation.DesignEvaluation;
import com.ho.account.internalaudit.core.domain.evaluation.OperatingEvaluation;
import com.ho.account.internalaudit.core.domain.rcm.ControlActivity;
import com.ho.account.internalaudit.core.domain.rcm.RcmProcess;
import com.ho.account.internalaudit.core.domain.rcm.RcmRisk;
import jakarta.persistence.EntityManager;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * HTTP requests finish their own Spring transactions before JDBC assertions run.
 * The PostgreSQL subclass reuses this contract against the isolated real database.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:command_idempotency;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.locations=classpath:db/migration",
        "spring.flyway.target=62",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("local")
class CommandIdempotencyIntegrationTest {
    protected static final String ACTOR = "test-auditor";
    protected static final String KEY = "command-test-key";
    protected static final ObjectMapper JSON = new ObjectMapper();
    protected static final List<String> BUSINESS_TABLES = List.of(
            "operating_evaluation_jpa_entity_evidence_file_paths", "eval_deficiency",
            "eval_operating", "eval_design", "rcm_control_activity", "rcm_risk", "rcm_process");

    @Autowired protected MockMvc mvc;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected EntityManager entityManager;
    @Autowired protected PlatformTransactionManager transactionManager;
    @SpyBean protected RcmPersistencePort rcm;
    @SpyBean protected EvaluationPersistencePort evaluations;
    @SpyBean protected AuditLogPersistencePort audits;
    @SpyBean protected CommandReceiptPort receipts;

    @BeforeEach
    void prepareCommittedParents() {
        AuditActorContext.clear();
        jdbc.update("DELETE FROM internal_audit_command_receipt");
        jdbc.update("DELETE FROM internal_audit_log");
        deleteBusinessRows();
        for (int index = 1; index <= 2; index++) {
            jdbc.update("INSERT INTO rcm_process(process_id, process_name) VALUES (?, ?)",
                    "parent-process-" + index, "Parent process " + index);
            jdbc.update("INSERT INTO rcm_risk(risk_id, process_id) VALUES (?, ?)",
                    "parent-risk-" + index, "parent-process-" + index);
            jdbc.update("INSERT INTO rcm_control_activity(control_id, risk_id) VALUES (?, ?)",
                    "parent-control-" + index, "parent-risk-" + index);
            jdbc.update("INSERT INTO eval_design(evaluation_id, control_id, result) VALUES (?, ?, ?)",
                    "parent-evaluation-" + index, "parent-control-" + index, "EFFECTIVE");
        }
        clearStorageInvocations();
    }

    @AfterEach
    void clearActorContext() {
        AuditActorContext.clear();
    }

    @ParameterizedTest
    @EnumSource(Command.class)
    void exactHttpReplayReturnsFirstResponseWithoutAnyWrites(Command command) throws Exception {
        ObjectNode body = command.body();
        JsonNode first = successful(postCommand(command.path, body, " " + KEY + " ", " " + ACTOR + " ", "first-trace"));
        assertThat(first).isEqualTo(command.effectiveBody());
        Map<String, List<List<String>>> before = databaseSnapshot();
        clearStorageInvocations();

        JsonNode replay = successful(postCommand(command.path, reversed(body), KEY, ACTOR, "retry-trace"));

        assertThat(replay).isEqualTo(first);
        assertThat(databaseSnapshot()).isEqualTo(before);
        assertNoWrites();
        assertThat(audits.findByCorrelationId("first-trace")).singleElement().satisfies(entry -> {
            assertThat(entry.actor()).isEqualTo(ACTOR);
            assertThat(entry.action()).isEqualTo(command.action);
            assertThat(entry.aggregateType()).isEqualTo(command.aggregateType);
            assertThat(entry.aggregateId()).isEqualTo("target-1");
            assertThat(entry.idempotencyKey()).isEqualTo(KEY);
        });
        assertThat(audits.findByCorrelationId("retry-trace")).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("conflictingCommands")
    void changedActorActionTargetPayloadOrParentConflictsWithoutWrites(Command command, Change change) throws Exception {
        successful(postCommand(command, KEY));
        Map<String, List<List<String>>> before = databaseSnapshot();
        clearStorageInvocations();
        ObjectNode changed = command.body();
        String path = command.path;
        String actor = ACTOR;
        switch (change) {
            case ACTOR -> actor = "other-auditor";
            case ACTION -> {
                Command other = Command.values()[(command.ordinal() + 1) % Command.values().length];
                changed = other.body();
                path = other.path;
            }
            case TARGET -> changed.put(command.idField, "target-2");
            case PAYLOAD -> changed.put(command.textField, "Changed business payload");
            case PARENT -> {
                if (command == Command.RISK || command == Command.CONTROL) {
                    path = path.replace("-1/", "-2/");
                } else {
                    changed.put(command.parentField, command.parentId.replace("-1", "-2"));
                }
            }
        }

        assertConflict(postCommand(path, changed, KEY, actor, "changed-trace"));

        assertThat(databaseSnapshot()).isEqualTo(before);
        assertNoWrites();
    }

    @ParameterizedTest
    @EnumSource(Command.class)
    void staleKeyReplaysOldResponseAndPreservesLaterCommittedUpdate(Command command) throws Exception {
        JsonNode first = successful(postCommand(command, KEY));
        ObjectNode later = command.body().put(command.textField, "Later committed value");
        JsonNode latest = successful(postCommand(command.path, later, "later-key", ACTOR, "later-trace"));
        assertThat(latest.path(command.textField).asText()).isEqualTo("Later committed value");
        Map<String, List<List<String>>> before = databaseSnapshot();
        clearStorageInvocations();

        assertThat(successful(postCommand(command, KEY))).isEqualTo(first);

        assertThat(databaseSnapshot()).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT " + command.textColumn + " FROM " + command.table
                + " WHERE " + command.idColumn + " = ?", String.class, "target-1"))
                .isEqualTo("Later committed value");
        assertThat(audits.findByAggregate(command.aggregateType, "target-1")).hasSize(2);
        assertThat(audits.findByCorrelationId("later-trace")).hasSize(1);
        assertNoWrites();
    }

    @ParameterizedTest
    @EnumSource(Command.class)
    void replayDoesNotDependOnCurrentBusinessRowsOrParentExistence(Command command) throws Exception {
        JsonNode first = successful(postCommand(command, KEY));
        deleteBusinessRows();
        Map<String, List<List<String>>> before = databaseSnapshot();
        clearStorageInvocations();

        assertThat(successful(postCommand(command, KEY))).isEqualTo(first);

        assertThat(databaseSnapshot()).isEqualTo(before);
        assertNoWrites();
    }

    @ParameterizedTest
    @MethodSource("unkeyedCommands")
    void absentEmptyAndWhitespaceKeysExecuteEveryTimeWithoutReceipts(Command command, String key) throws Exception {
        successful(postCommand(command, key));
        successful(postCommand(command, key));

        assertThat(audits.findByAggregate(command.aggregateType, "target-1")).hasSize(2)
                .allSatisfy(entry -> assertThat(entry.idempotencyKey()).isNull());
        assertThat(rowCount("internal_audit_command_receipt")).isZero();
        verify(receipts, never()).reserve(anyString(), anyInt(), anyString());
        verify(receipts, never()).complete(anyString(), anyInt(), anyString());
    }

    @ParameterizedTest
    @EnumSource(Command.class)
    void effectiveDefaultsTrustedActorsAndPathValuesDefineIdentity(Command command) throws Exception {
        ObjectNode firstBody = command.body();
        if (command == Command.PROCESS || command == Command.CONTROL) {
            firstBody.put("ownerId", " ");
        }
        if (command == Command.DESIGN || command == Command.OPERATING) {
            firstBody.put("result", " effective ").put("evaluatorId", "untrusted-initial");
        }
        JsonNode first = successful(postCommand(command.path, firstBody, KEY, ACTOR, "first-trace"));
        ObjectNode equivalent = command.effectiveBody();
        if (command == Command.DESIGN || command == Command.OPERATING) {
            equivalent.put("evaluatorId", "untrusted-retry");
        }
        Map<String, List<List<String>>> before = databaseSnapshot();
        clearStorageInvocations();

        assertThat(successful(postCommand(command.path, reversed(equivalent), KEY, ACTOR, "retry-trace")))
                .isEqualTo(first);

        assertThat(databaseSnapshot()).isEqualTo(before);
        assertNoWrites();
    }

    @ParameterizedTest
    @MethodSource("legacyCommands")
    void legacyAuditKeyAlwaysConflictsRegardlessOfDetails(Command command, String details) throws Exception {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> audits.append(
                auditEvent(command, KEY, details)));
        Map<String, List<List<String>>> before = databaseSnapshot();
        clearStorageInvocations();

        assertConflict(postCommand(command, KEY));

        assertThat(databaseSnapshot()).isEqualTo(before);
        assertNoWrites();
    }

    @ParameterizedTest
    @MethodSource("damagedReceipts")
    void invalidReceiptFailsClosedWithoutBusinessExecution(Command command, ReceiptDamage damage) throws Exception {
        JsonNode original = successful(postCommand(command, KEY));
        switch (damage) {
            case INCOMPLETE -> jdbc.update("UPDATE internal_audit_command_receipt SET snapshot_version=0, snapshot_json=NULL");
            case INVALID_JSON -> jdbc.update("UPDATE internal_audit_command_receipt SET snapshot_json='invalid-json'");
            case NULL_RESULT -> jdbc.update("UPDATE internal_audit_command_receipt SET snapshot_json='null'");
            case MISSING_FIELDS -> jdbc.update("UPDATE internal_audit_command_receipt SET snapshot_json='{}'");
            case FUTURE_FINGERPRINT -> jdbc.update("UPDATE internal_audit_command_receipt SET fingerprint_version=99");
            case FUTURE_SNAPSHOT -> jdbc.update("UPDATE internal_audit_command_receipt SET snapshot_version=99");
            case CHANGED_IDENTIFIER -> jdbc.update("UPDATE internal_audit_command_receipt SET snapshot_json=?",
                    ((ObjectNode) original.deepCopy()).put(command.idField, "tampered-target").toString());
            case CHANGED_OPTIONAL_FIELD -> jdbc.update("UPDATE internal_audit_command_receipt SET snapshot_json=?",
                    ((ObjectNode) original.deepCopy()).putNull(command.textField).toString());
            case MISSING_AUDIT -> jdbc.update("DELETE FROM internal_audit_log");
        }
        Map<String, List<List<String>>> before = databaseSnapshot();
        clearStorageInvocations();

        assertInternalError(postCommand(command, KEY));

        assertThat(databaseSnapshot()).isEqualTo(before);
        assertNoWrites();
    }

    @ParameterizedTest
    @MethodSource("failingCommands")
    void allCommandStagesRollbackRealWritesAndAllowSameKeyRetry(Command command, FailureStage stage) throws Exception {
        Map<String, List<List<String>>> before = databaseSnapshot();
        installFailure(command, stage);

        assertInternalError(postCommand(command, KEY));

        // These queries use a fresh connection after the request transaction ended.
        assertThat(databaseSnapshot()).isEqualTo(before);
        if (stage == FailureStage.RECEIPT_RESERVE) {
            verify(receipts).reserve(anyString(), anyInt(), anyString());
        } else {
            verifyBusinessSave(command);
        }
        if (stage == FailureStage.AUDIT_APPEND || stage == FailureStage.RECEIPT_COMPLETE) {
            verify(audits).append(any());
        }
        if (stage == FailureStage.RECEIPT_COMPLETE) {
            verify(receipts).complete(anyString(), anyInt(), anyString());
        }
        if (stage == FailureStage.SNAPSHOT_SERIALIZATION) {
            verify(audits, never()).append(any());
            verify(receipts, never()).complete(anyString(), anyInt(), anyString());
        }
        org.mockito.Mockito.reset(rcm, evaluations, audits, receipts);
        successful(postCommand(command, KEY));
        assertThat(audits.findByAggregate(command.aggregateType, "target-1")).hasSize(1);
        assertThat(rowCount("internal_audit_command_receipt")).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("operatingUnspecifiedCounts")
    void independentlyUnspecifiedCountsRemainDistinctFromZero(String field) throws Exception {
        ObjectNode body = Command.OPERATING.body();
        body.putNull("sampleSize").putNull("exceptionCount");
        JsonNode first = successful(postCommand(Command.OPERATING.path, body, KEY, ACTOR, "first-trace"));
        assertThat(first.path("sampleSize").isNull()).isTrue();
        assertThat(first.path("exceptionCount").isNull()).isTrue();
        Map<String, List<List<String>>> before = databaseSnapshot();
        clearStorageInvocations();

        assertConflict(postCommand(Command.OPERATING.path, body.put(field, 0), KEY, ACTOR, "retry-trace"));

        assertThat(databaseSnapshot()).isEqualTo(before);
        assertNoWrites();
    }

    @Test
    void evidenceOrderAndWhitespaceInBusinessTextAreSignificant() throws Exception {
        successful(postCommand(Command.OPERATING, KEY));
        ObjectNode reordered = Command.OPERATING.body();
        reordered.putArray("evidenceFilePaths").add("evidence-b").add("evidence-a");
        Map<String, List<List<String>>> before = databaseSnapshot();
        clearStorageInvocations();

        assertConflict(postCommand(Command.OPERATING.path, reordered, KEY, ACTOR, "retry-trace"));
        ObjectNode spaced = Command.OPERATING.body().put("remarks", " Original text ");
        assertConflict(postCommand(Command.OPERATING.path, spaced, KEY, ACTOR, "retry-trace"));

        assertThat(databaseSnapshot()).isEqualTo(before);
        assertNoWrites();
    }

    @Test
    void replayStillRequiresAuthenticationAuthorizationAndValidPathBody() throws Exception {
        successful(postCommand(Command.RISK, KEY));
        Map<String, List<List<String>>> before = databaseSnapshot();
        clearStorageInvocations();

        assertThat(postCommand(Command.RISK.path, Command.RISK.body(), KEY, " ", "retry").getResponse().getStatus())
                .isEqualTo(401);
        assertThat(mvc.perform(post(Command.RISK.path).header("X-Auth-User", ACTOR)
                .header("X-Auth-Roles", "ROLE_USER").header("X-Idempotency-Key", KEY)
                .contentType(MediaType.APPLICATION_JSON).content(Command.RISK.body().toString()))
                .andReturn().getResponse().getStatus()).isEqualTo(401);
        assertThat(postCommand(Command.RISK.path, Command.RISK.body().put("processId", "different-parent"),
                KEY, ACTOR, "retry").getResponse().getStatus()).isEqualTo(400);

        assertThat(databaseSnapshot()).isEqualTo(before);
        assertNoWrites();
    }

    protected void installFailure(Command command, FailureStage stage) throws Exception {
        // Configure the spy without invoking MANDATORY advice outside a transaction.
        // Production requests still enter the original receipt proxy and real transaction.
        CommandReceiptPort receiptTarget = AopTestUtils.getUltimateTargetObject(receipts);
        Answer<Object> afterWriteFailure = invocation -> {
            invocation.callRealMethod();
            entityManager.flush();
            assertThat(rowCount("internal_audit_command_receipt")).isEqualTo(1);
            if (stage != FailureStage.RECEIPT_RESERVE) {
                assertBusinessRowWritten(command);
            }
            if (stage == FailureStage.AUDIT_APPEND || stage == FailureStage.RECEIPT_COMPLETE) {
                assertThat(rowCount("internal_audit_log")).isEqualTo(1);
            }
            throw new IllegalStateException("synthetic-failure-private-detail");
        };
        switch (stage) {
            case BUSINESS_SAVE -> interceptBusinessSave(command, afterWriteFailure);
            case AUDIT_APPEND -> doAnswer(afterWriteFailure).when(audits).append(any());
            case RECEIPT_RESERVE -> doAnswer(afterWriteFailure).when(receiptTarget).reserve(anyString(), anyInt(), anyString());
            case RECEIPT_COMPLETE -> doAnswer(afterWriteFailure).when(receiptTarget).complete(anyString(), anyInt(), anyString());
            case SNAPSHOT_SERIALIZATION -> interceptBusinessSave(command, invocation -> {
                Object saved = invocation.callRealMethod();
                entityManager.flush();
                assertBusinessRowWritten(command);
                return poisonSnapshotAccessor(command, saved);
            });
        }
    }

    protected void interceptBusinessSave(Command command, Answer<?> answer) {
        switch (command) {
            case PROCESS -> doAnswer(answer).when(rcm).saveProcess(any());
            case RISK -> doAnswer(answer).when(rcm).saveRisk(any());
            case CONTROL -> doAnswer(answer).when(rcm).saveControlActivity(any());
            case DESIGN -> doAnswer(answer).when(evaluations).saveDesignEvaluation(any());
            case OPERATING -> doAnswer(answer).when(evaluations).saveOperatingEvaluation(any());
            case DEFICIENCY -> doAnswer(answer).when(evaluations).saveDeficiency(any());
        }
    }

    private void verifyBusinessSave(Command command) {
        switch (command) {
            case PROCESS -> verify(rcm).saveProcess(any());
            case RISK -> verify(rcm).saveRisk(any());
            case CONTROL -> verify(rcm).saveControlActivity(any());
            case DESIGN -> verify(evaluations).saveDesignEvaluation(any());
            case OPERATING -> verify(evaluations).saveOperatingEvaluation(any());
            case DEFICIENCY -> verify(evaluations).saveDeficiency(any());
        }
    }

    private void assertBusinessRowWritten(Command command) {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + command.table
                + " WHERE " + command.idColumn + "=?", Long.class, "target-1")).isEqualTo(1);
    }

    private Object poisonSnapshotAccessor(Command command, Object saved) {
        Object poisoned = spy(saved);
        IllegalStateException failure = new IllegalStateException("synthetic-snapshot-private-detail");
        // The real row was written first. Only serialization of the returned result fails.
        switch (command) {
            case PROCESS -> doThrow(failure).when((RcmProcess) poisoned).description();
            case RISK -> doThrow(failure).when((RcmRisk) poisoned).riskDescription();
            case CONTROL -> doThrow(failure).when((ControlActivity) poisoned).controlDescription();
            case DESIGN -> doThrow(failure).when((DesignEvaluation) poisoned).remarks();
            case OPERATING -> doThrow(failure).when((OperatingEvaluation) poisoned).remarks();
            case DEFICIENCY -> doThrow(failure).when((Deficiency) poisoned).description();
        }
        return poisoned;
    }

    protected MvcResult postCommand(Command command, String key) throws Exception {
        return postCommand(command.path, command.body(), key, ACTOR, "first-trace");
    }

    protected MvcResult postCommand(String path, ObjectNode body, String key, String actor, String trace) throws Exception {
        MockHttpServletRequestBuilder request = post(path).header("X-Auth-User", actor)
                .header("X-Auth-Roles", "ROLE_AUDITOR").header("X-Correlation-Id", trace)
                .contentType(MediaType.APPLICATION_JSON).content(body.toString());
        if (actor != null && !actor.isBlank()) {
            request.requestAttr(InternalAuditIdentityFilter.PRINCIPAL_ATTRIBUTE,
                    new InternalAuditPrincipal(actor, List.of("ROLE_AUDITOR"), 1));
        }
        if (key != null) {
            request.header("X-Idempotency-Key", key);
        }
        return mvc.perform(request).andReturn();
    }

    protected JsonNode successful(MvcResult response) throws Exception {
        assertThat(response.getResponse().getStatus()).as(response.getResponse().getContentAsString()).isEqualTo(200);
        return JSON.readTree(response.getResponse().getContentAsString());
    }

    protected void assertConflict(MvcResult response) throws Exception {
        assertThat(response.getResponse().getStatus()).isEqualTo(409);
        assertThat(JSON.readTree(response.getResponse().getContentAsString())).isEqualTo(JSON.readTree("""
                {"code":"IDEMPOTENCY_CONFLICT","message":"Idempotency key cannot be reused for this command."}
                """));
    }

    protected void assertInternalError(MvcResult response) throws Exception {
        assertThat(response.getResponse().getStatus()).isEqualTo(500);
        assertThat(JSON.readTree(response.getResponse().getContentAsString())).isEqualTo(JSON.readTree("""
                {"code":"INTERNAL_AUDIT_ERROR","message":"Unable to process the command."}
                """));
    }

    protected AuditLogEntry auditEvent(Command command, String key, String details) {
        return AuditLogEntry.builder().actor(ACTOR).action(command.action).aggregateType(command.aggregateType)
                .aggregateId("target-1").idempotencyKey(key).correlationId("legacy-trace").detailsJson(details).build();
    }

    protected void clearStorageInvocations() {
        clearInvocations(rcm, evaluations, audits, receipts);
    }

    protected void assertNoWrites() {
        verify(rcm, never()).saveProcess(any());
        verify(rcm, never()).saveRisk(any());
        verify(rcm, never()).saveControlActivity(any());
        verify(evaluations, never()).saveDesignEvaluation(any());
        verify(evaluations, never()).saveOperatingEvaluation(any());
        verify(evaluations, never()).saveDeficiency(any());
        verify(audits, never()).append(any());
        verify(receipts, never()).reserve(anyString(), anyInt(), anyString());
        verify(receipts, never()).complete(anyString(), anyInt(), anyString());
    }

    protected long rowCount(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    protected void deleteBusinessRows() {
        BUSINESS_TABLES.forEach(table -> jdbc.update("DELETE FROM " + table));
    }

    protected Map<String, List<List<String>>> databaseSnapshot() {
        List<String> tables = new ArrayList<>(BUSINESS_TABLES);
        tables.addAll(List.of("internal_audit_log", "internal_audit_command_receipt", "internal_audit_key_lock"));
        Map<String, List<List<String>>> snapshot = new LinkedHashMap<>();
        for (String table : tables) {
            List<List<String>> rows = jdbc.query("SELECT * FROM " + table, (row, rowNumber) -> {
                ResultSetMetaData columns = row.getMetaData();
                List<String> values = new ArrayList<>();
                for (int column = 1; column <= columns.getColumnCount(); column++) {
                    values.add(row.getString(column));
                }
                return values;
            });
            rows.sort((left, right) -> left.toString().compareTo(right.toString()));
            snapshot.put(table, rows);
        }
        return snapshot;
    }

    private static ObjectNode reversed(ObjectNode original) {
        List<String> names = new ArrayList<>();
        original.fieldNames().forEachRemaining(names::add);
        Collections.reverse(names);
        ObjectNode reversed = JSON.createObjectNode();
        names.forEach(name -> reversed.set(name, original.get(name)));
        return reversed;
    }

    static Stream<Arguments> conflictingCommands() {
        return Stream.of(Command.values()).flatMap(command -> Stream.of(Change.values())
                .filter(change -> change != Change.PARENT || command != Command.PROCESS)
                .map(change -> Arguments.of(command, change)));
    }

    static Stream<Arguments> unkeyedCommands() {
        return Stream.of(Command.values()).flatMap(command -> Stream.of(null, "", "   ")
                .map(key -> Arguments.of(command, key)));
    }

    static Stream<Arguments> legacyCommands() {
        return Stream.of(Command.values()).flatMap(command -> Stream.of(null, "{\"old\":true}", "old fallback text")
                .map(details -> Arguments.of(command, details)));
    }

    static Stream<Arguments> damagedReceipts() {
        return Stream.of(Command.values()).flatMap(command -> Stream.of(ReceiptDamage.values())
                .map(damage -> Arguments.of(command, damage)));
    }

    static Stream<Arguments> failingCommands() {
        return Stream.of(Command.values()).flatMap(command -> Stream.of(FailureStage.values())
                .map(stage -> Arguments.of(command, stage)));
    }

    static Stream<String> operatingUnspecifiedCounts() {
        return Stream.of("sampleSize", "exceptionCount");
    }

    enum Change { ACTOR, ACTION, TARGET, PAYLOAD, PARENT }
    enum ReceiptDamage {
        INCOMPLETE, INVALID_JSON, NULL_RESULT, MISSING_FIELDS, FUTURE_FINGERPRINT, FUTURE_SNAPSHOT,
        CHANGED_IDENTIFIER, CHANGED_OPTIONAL_FIELD, MISSING_AUDIT
    }
    enum FailureStage { BUSINESS_SAVE, AUDIT_APPEND, RECEIPT_RESERVE, RECEIPT_COMPLETE, SNAPSHOT_SERIALIZATION }

    protected enum Command {
        PROCESS("/rcms/processes", "CREATE_PROCESS", "RCM_PROCESS", "rcm_process", "processId", "process_id",
                "description", "description", null, null),
        RISK("/rcms/processes/parent-process-1/risks", "ADD_RISK", "RCM_RISK", "rcm_risk", "riskId", "risk_id",
                "riskDescription", "risk_description", "processId", "parent-process-1"),
        CONTROL("/rcms/risks/parent-risk-1/controls", "ADD_CONTROL", "CONTROL_ACTIVITY", "rcm_control_activity",
                "controlId", "control_id", "controlDescription", "control_description", "riskId", "parent-risk-1"),
        DESIGN("/evaluations/design", "SUBMIT_DESIGN_EVALUATION", "DESIGN_EVALUATION", "eval_design",
                "evaluationId", "evaluation_id", "remarks", "remarks", "controlId", "parent-control-1"),
        OPERATING("/evaluations/operating", "SUBMIT_OPERATING_EVALUATION", "OPERATING_EVALUATION", "eval_operating",
                "evaluationId", "evaluation_id", "remarks", "remarks", "controlId", "parent-control-1"),
        DEFICIENCY("/evaluations/deficiencies", "REGISTER_DEFICIENCY", "DEFICIENCY", "eval_deficiency",
                "deficiencyId", "deficiency_id", "description", "description", "evaluationId", "parent-evaluation-1");

        final String path;
        final String action;
        final String aggregateType;
        final String table;
        final String idField;
        final String idColumn;
        final String textField;
        final String textColumn;
        final String parentField;
        final String parentId;

        Command(String path, String action, String aggregateType, String table, String idField, String idColumn,
                String textField, String textColumn, String parentField, String parentId) {
            this.path = "/api/v1/internalaudit" + path;
            this.action = action;
            this.aggregateType = aggregateType;
            this.table = table;
            this.idField = idField;
            this.idColumn = idColumn;
            this.textField = textField;
            this.textColumn = textColumn;
            this.parentField = parentField;
            this.parentId = parentId;
        }

        ObjectNode body() {
            ObjectNode body = JSON.createObjectNode().put(idField, "target-1").put(textField, "Original text");
            if (parentField != null) {
                body.put(parentField, parentId);
            }
            switch (this) {
                case PROCESS -> body.put("processName", "Original process").putNull("ownerId");
                case RISK -> body.putNull("processId").put("impactLevel", "HIGH").put("likelihood", "POSSIBLE");
                case CONTROL -> body.putNull("riskId").put("controlType", "DETECTIVE")
                        .put("executionMethod", "MANUAL").put("frequency", "MONTHLY").putNull("ownerId");
                case DESIGN, OPERATING -> {
                    body.put("evaluatorId", "untrusted-body-actor").put("evaluationDate", "2026-09-12")
                            .put("result", "EFFECTIVE");
                    if (this == OPERATING) {
                        body.put("sampleSize", 10).put("exceptionCount", 0)
                                .putArray("evidenceFilePaths").add("evidence-a").add("evidence-b");
                    }
                }
                case DEFICIENCY -> body.put("remediationPlan", "Update checklist").put("status", "IDENTIFIED");
            }
            return body;
        }

        ObjectNode effectiveBody() {
            ObjectNode body = body();
            if (this == PROCESS || this == CONTROL) {
                body.put("ownerId", ACTOR);
            }
            if (this == RISK || this == CONTROL) {
                body.put(parentField, parentId);
            }
            if (this == DESIGN || this == OPERATING) {
                body.put("evaluatorId", ACTOR);
            }
            return body;
        }
    }
}
