package com.ho.account.masterdata.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import com.ho.account.masterdata.core.application.port.in.MasterDataChangeRequestUseCase;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.exception.MasterDataIdempotencyConflictException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Real PostgreSQL gate: each run uses a new schema and never modifies a pre-existing table. */
@EnabledIfEnvironmentVariable(named = "MASTER_DATA_TEST_POSTGRES_URL", matches = "jdbc:postgresql:.*")
@SpringBootTest(classes = MasterDataChangeRequestPostgresqlIdempotencyTest.TestApplication.class,
        properties = {
                "spring.config.name=change-request-postgresql-test",
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                "spring.datasource.hikari.maximum-pool-size=8",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.clean-disabled=true",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "spring.sql.init.mode=never",
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.import-check.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false"
        })
@Timeout(40)
class MasterDataChangeRequestPostgresqlIdempotencyTest {

    private static final String SCHEMA = "change_request_test_" + UUID.randomUUID().toString().replace("-", "");

    @Autowired private MasterDataChangeRequestUseCase requests;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("MASTER_DATA_TEST_POSTGRES_URL"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.hikari.schema", () -> SCHEMA);
        registry.add("spring.flyway.schemas", () -> SCHEMA);
        registry.add("spring.flyway.default-schema", () -> SCHEMA);
    }

    @Test
    void concurrentIdenticalFirstSubmissionsReturnOneRequest() throws Exception {
        assertThat(jdbc.queryForObject("SELECT version()", String.class)).startsWith("PostgreSQL");
        String key = key();
        String source = "source-" + key;
        MasterDataChangeRequestCommand command = createCommand(key, source, "Department " + key);
        assertContendedSubmission(command, command, false);
    }

    @Test
    void concurrentMismatchedSourceReferenceReturnsConflictBeforeBusinessVersionCheck() throws Exception {
        String source = "source-" + key();
        MasterDataChangeRequestCommand first = createCommand(key(), source, "First department");
        MasterDataChangeRequestCommand different = createCommand(key(), source, "Different department");
        assertContendedSubmission(first, different, true);
    }

    private void assertContendedSubmission(MasterDataChangeRequestCommand firstCommand,
            MasterDataChangeRequestCommand secondCommand, boolean expectConflict) throws Exception {
        CountDownLatch inserted = new CountDownLatch(1);
        CountDownLatch commit = new CountDownLatch(1);
        CountDownLatch contenderStarted = new CountDownLatch(1);
        AtomicInteger contenderBackend = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<MasterDataChangeRequest> first = executor.submit(() -> {
                return new TransactionTemplate(transactionManager).execute(status -> {
                    MasterDataChangeRequest created = requests.requestChange(firstCommand);
                    inserted.countDown();
                    await(commit);
                    return created;
                });
            });
            await(inserted);
            Future<MasterDataChangeRequest> second = executor.submit(() -> {
                return new TransactionTemplate(transactionManager).execute(status -> {
                    contenderBackend.set(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
                    contenderStarted.countDown();
                    return requests.requestChange(secondCommand);
                });
            });
            await(contenderStarted);
            awaitBlockedOnInsert(contenderBackend.get());
            assertThat(second.isDone()).as("contender waits for the uncommitted source_reference").isFalse();
            commit.countDown();
            MasterDataChangeRequest left = first.get(20, TimeUnit.SECONDS);
            assertThat(left.getStatus()).isEqualTo(ChangeStatus.REQUESTED);
            if (expectConflict) {
                assertThatThrownBy(() -> second.get(20, TimeUnit.SECONDS))
                        .hasCauseInstanceOf(MasterDataIdempotencyConflictException.class);
            } else {
                MasterDataChangeRequest right = second.get(20, TimeUnit.SECONDS);
                assertThat(left.getId()).isEqualTo(right.getId());
                assertThat(left.getRequestedAt()).isEqualTo(right.getRequestedAt());
                assertThat(right.getStatus()).isEqualTo(ChangeStatus.REQUESTED);
            }
            assertThat(requestCount(firstCommand.sourceReference())).isEqualTo(1);
            assertThat(jdbc.queryForObject(
                    "SELECT target_key FROM master_data_change_requests WHERE source_reference = ?",
                    String.class, firstCommand.sourceReference())).isEqualTo(firstCommand.targetKey());
        } finally {
            commit.countDown();
            executor.shutdownNow();
        }
    }

    private void awaitBlockedOnInsert(int backend) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < deadline) {
            Boolean blocked = jdbc.queryForObject(
                    "SELECT cardinality(pg_blocking_pids(?)) > 0", Boolean.class, backend);
            if (Boolean.TRUE.equals(blocked)) {
                String query = jdbc.queryForObject(
                        "SELECT query FROM pg_stat_activity WHERE pid = ?", String.class, backend);
                assertThat(query).contains("INSERT INTO master_data_change_requests");
                return;
            }
            Thread.sleep(25);
        }
        throw new AssertionError("Contender never blocked on PostgreSQL source_reference insert");
    }

    @Test
    void applyResponseLossRetryKeepsAppliedTimestampAndScd2VersionCount() {
        String key = key();
        String source = "source-" + key;
        MasterDataChangeRequest requested = requests.requestChange(
                createCommand(key, source, "Department " + key));
        requests.approve(requested.getId(), "approver");
        MasterDataChangeRequest applied = requests.applyApprovedChange(requested.getId());
        // PostgreSQL TIMESTAMP stores microseconds, while Java's first in-memory value has nanoseconds.
        LocalDateTime appliedAt = jdbc.queryForObject(
                "SELECT applied_at FROM master_data_change_requests WHERE id = ?",
                LocalDateTime.class, requested.getId());
        assertThat(applied.getAppliedAt()).isNotNull();
        int versionCount = jdbc.queryForObject("SELECT count(*) FROM departments WHERE code = ?",
                Integer.class, key);

        MasterDataChangeRequest retry = requests.applyApprovedChange(requested.getId());

        assertThat(retry.getId()).isEqualTo(applied.getId());
        assertThat(retry.getStatus()).isEqualTo(ChangeStatus.APPLIED);
        assertThat(retry.getAppliedAt()).isEqualTo(appliedAt);
        assertThat(jdbc.queryForObject(
                "SELECT applied_at FROM master_data_change_requests WHERE id = ?",
                LocalDateTime.class, requested.getId())).isEqualTo(appliedAt);
        assertThat(versionCount).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM departments WHERE code = ?",
                Integer.class, key)).isEqualTo(versionCount);
        assertThat(requestCount(source)).isEqualTo(1);
        assertThat(requests.requestChange(createCommand(key, source, "Department " + key)).getId())
                .isEqualTo(requested.getId());
    }

    @Test
    void rejectedRequestReplaysOriginalCommandAndLegacyOverwrittenReasonFailsClosed() {
        String key = key();
        String source = "source-" + key;
        MasterDataChangeRequestCommand original = createCommand(key, source, "Department " + key);
        MasterDataChangeRequest requested = requests.requestChange(original);
        MasterDataChangeRequest rejected = requests.reject(requested.getId(), "approver", "policy rejected");

        assertThat(rejected.getStatus()).isEqualTo(ChangeStatus.REJECTED);
        assertThat(rejected.getReason()).isEqualTo("create");
        assertThat(rejected.getDecisionReason()).isEqualTo("policy rejected");
        assertThat(rejected.getRejectionReason()).isEqualTo("policy rejected");
        assertThat(rejected.getRequestReason()).isEqualTo("create");
        MasterDataChangeRequest replayed = requests.requestChange(original);
        assertThat(replayed.getId()).isEqualTo(requested.getId());
        assertThat(replayed.getStatus()).isEqualTo(ChangeStatus.REJECTED);
        assertThat(replayed.getReason()).isEqualTo("create");
        assertThat(replayed.getDecisionReason()).isEqualTo("policy rejected");
        assertThat(replayed.getRejectionReason()).isEqualTo("policy rejected");
        assertThat(jdbc.queryForObject(
                "SELECT reason FROM master_data_change_requests WHERE id = ?",
                String.class, requested.getId())).isEqualTo("create");
        assertThat(jdbc.queryForObject(
                "SELECT rejection_reason FROM master_data_change_requests WHERE id = ?",
                String.class, requested.getId())).isEqualTo("policy rejected");

        MasterDataChangeRequestCommand changedReason = new MasterDataChangeRequestCommand(
                original.targetType(), original.targetKey(), original.changeType(),
                original.effectiveDate(), original.requestedVersion(), original.requestedBy(),
                "changed request reason", original.payloadJson(), original.sourceReference());
        assertThatThrownBy(() -> requests.requestChange(changedReason))
                .isInstanceOf(MasterDataIdempotencyConflictException.class);

        // Old rejected rows stored the rejection reason over the request reason. Their original
        // immutable command cannot be reconstructed, so replay must fail closed.
        jdbc.update("UPDATE master_data_change_requests SET reason = ?, rejection_reason = NULL, decision_reason = NULL WHERE id = ?",
                "policy rejected", requested.getId());
        assertThatThrownBy(() -> requests.requestChange(original))
                .isInstanceOf(MasterDataIdempotencyConflictException.class);
        assertThat(requestCount(source)).isEqualTo(1);
    }

    @Test
    void uniqueViolationAbortsPostgresqlTransactionButConflictSafeRetryCanReadCommittedWinner() throws Exception {
        String source = "raw-" + key();
        String firstKey = key();
        String secondKey = key();
        String insert = """
                INSERT INTO master_data_change_requests
                (target_type, target_key, change_type, status, lock_version, effective_date,
                 requested_version, requested_by, requested_at, payload_json, source_reference)
                VALUES ('DEPARTMENT', ?, 'CREATE', 'REQUESTED', 0, CURRENT_DATE,
                        1, 'requester', CURRENT_TIMESTAMP, '{}', ?)
                """;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(insert)) {
                statement.setString(1, firstKey);
                statement.setString(2, source);
                statement.executeUpdate();
                connection.commit();

                statement.setString(1, secondKey);
                assertThatThrownBy(statement::executeUpdate)
                        .isInstanceOf(SQLException.class)
                        .extracting(error -> ((SQLException) error).getSQLState())
                        .isEqualTo("23505");
                assertThatThrownBy(() -> connection.createStatement().executeQuery("SELECT 1"))
                        .isInstanceOf(SQLException.class)
                        .extracting(error -> ((SQLException) error).getSQLState())
                        .isEqualTo("25P02");
                connection.rollback();
            }
        }

        // A separate transaction can recover the committed winner with PostgreSQL's atomic clause.
        assertThat(jdbc.query("""
                INSERT INTO master_data_change_requests
                (target_type, target_key, change_type, status, lock_version, effective_date,
                 requested_version, requested_by, requested_at, payload_json, source_reference)
                VALUES ('DEPARTMENT', ?, 'CREATE', 'REQUESTED', 0, CURRENT_DATE,
                        1, 'requester', CURRENT_TIMESTAMP, '{}', ?)
                ON CONFLICT (source_reference) DO NOTHING
                RETURNING id
                """, (result, rowNum) -> result.getLong(1), secondKey, source)).isEmpty();
        assertThat(requestCount(source)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT target_key FROM master_data_change_requests WHERE source_reference = ?",
                String.class, source)).isEqualTo(firstKey);
    }

    private MasterDataChangeRequestCommand createCommand(String key, String source, String name) {
        return new MasterDataChangeRequestCommand(MasterDataType.DEPARTMENT, key,
                ChangeType.CREATE, LocalDate.now().minusDays(1), 1, "requester", "create",
                "{\"code\":\"" + key + "\",\"name\":\"" + name + "\"}", source);
    }

    private int requestCount(String source) {
        return jdbc.queryForObject("SELECT count(*) FROM master_data_change_requests WHERE source_reference = ?",
                Integer.class, source);
    }

    private static String key() {
        return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 15);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) throw new AssertionError("Concurrent start timed out");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Concurrent start interrupted", exception);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @ComponentScan(
            basePackages = {"com.ho.account.masterdata.core.application",
                    "com.ho.account.masterdata.core.infrastructure"},
            excludeFilters = {
                    @ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.CUSTOM,
                            classes = org.springframework.boot.context.TypeExcludeFilter.class),
                    @ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                            classes = {
                                    MasterDataDueChangeIsolationTest.class,
                                    MasterDataDueChangeIsolationTest.TestApplication.class,
                                    MasterDataDueChangeIsolationTest.TestDependencies.class
                            })
            })
    @EntityScan(basePackages = {"com.ho.account.masterdata.core.domain.model",
            "com.ho.account.masterdata.core.domain.changerequest",
            "com.ho.account.masterdata.core.infrastructure.persistence"})
    @EnableJpaRepositories(basePackages = "com.ho.account.masterdata.core.infrastructure.persistence")
    static class TestApplication { }
}
