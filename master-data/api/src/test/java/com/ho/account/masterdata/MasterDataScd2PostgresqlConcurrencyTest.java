package com.ho.account.masterdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.masterdata.api.web.MasterDataChangeRequestController;
import com.ho.account.masterdata.api.web.MasterDataExceptionHandler;
import com.ho.account.masterdata.core.application.command.AccountSubjectCommand;
import com.ho.account.masterdata.core.application.command.BusinessPartnerCommand;
import com.ho.account.masterdata.core.application.command.DepartmentCommand;
import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import com.ho.account.masterdata.core.application.command.ProductCommand;
import com.ho.account.masterdata.core.application.port.in.AccountSubjectUseCase;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerUseCase;
import com.ho.account.masterdata.core.application.port.in.DepartmentUseCase;
import com.ho.account.masterdata.core.application.port.in.MasterDataChangeRequestUseCase;
import com.ho.account.masterdata.core.application.port.in.ProductUseCase;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangeRequestPersistencePort;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.domain.model.Product;
import com.ho.account.masterdata.core.infrastructure.persistence.JpaMasterDataVersionQueryAdapter;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Real PostgreSQL acceptance tests. The caller supplies an isolated synthetic database; each run
 * creates its own schema and never truncates a shared table. No H2 fallback can satisfy this gate.
 */
@EnabledIfEnvironmentVariable(named = "MASTER_DATA_TEST_POSTGRES_URL", matches = "jdbc:postgresql:.*")
@SpringBootTest(classes = MasterDataScd2PostgresqlConcurrencyTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
                "spring.config.name=scd2-postgresql-test",
                "spring.profiles.active=local",
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
class MasterDataScd2PostgresqlConcurrencyTest {

    private static final String SCHEMA = "scd2_test_" + UUID.randomUUID().toString().replace("-", "");
    private static final LocalDate START = LocalDate.now().minusDays(20);
    private static final LocalDate EFFECTIVE = LocalDate.now().minusDays(5);
    private static final LocalDate END = LocalDate.of(9999, 12, 31);

    @Autowired private MasterDataChangeRequestUseCase changeRequests;
    @Autowired private MasterDataChangeRequestPersistencePort requestPersistence;
    @Autowired private AccountSubjectUseCase accounts;
    @Autowired private BusinessPartnerUseCase partners;
    @Autowired private DepartmentUseCase departments;
    @Autowired private ProductUseCase products;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @SpyBean private JpaMasterDataVersionQueryAdapter versionQuery;

    private final AtomicReference<Thread> competingThread = new AtomicReference<>();
    private final AtomicBoolean ownerUncommitted = new AtomicBoolean();
    private final AtomicBoolean versionReadBeforeCommit = new AtomicBoolean();
    private MockMvc mvc;
    private TransactionTemplate transaction;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("MASTER_DATA_TEST_POSTGRES_URL"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.hikari.schema", () -> SCHEMA);
        registry.add("spring.flyway.schemas", () -> SCHEMA);
        registry.add("spring.flyway.default-schema", () -> SCHEMA);
    }

    @BeforeEach
    void configureRealServicesAndHttpAdvice() {
        transaction = new TransactionTemplate(transactionManager);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mvc = MockMvcBuilders.standaloneSetup(new MasterDataChangeRequestController(changeRequests),
                        new ConstraintProbeController())
                .setControllerAdvice(new MasterDataExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper)).build();
        competingThread.set(null);
        ownerUncommitted.set(false);
        versionReadBeforeCommit.set(false);
        // Observe the real adapter without replacing its query: a row-level fallback may block only
        // after it has already accepted a stale requestedVersion, which is too late for this contract.
        doAnswer(invocation -> {
            if (Thread.currentThread() == competingThread.get() && ownerUncommitted.get()) {
                versionReadBeforeCommit.set(true);
            }
            return invocation.callRealMethod();
        }).when(versionQuery).countPersistedVersions(any(MasterDataType.class), anyString());
        assertThat(jdbc.queryForObject("SELECT version()", String.class)).startsWith("PostgreSQL");
    }

    @ParameterizedTest
    @EnumSource(value = MasterDataType.class, names = {"ACCOUNT_SUBJECT", "BUSINESS_PARTNER", "DEPARTMENT", "PRODUCT"})
    void distinctApprovedCreatesSerializeEvenWhenBusinessKeyHasNoRows(MasterDataType type) throws Exception {
        verifyApprovedRace(type, ChangeType.CREATE, ChangeType.CREATE);
    }

    @ParameterizedTest
    @EnumSource(value = MasterDataType.class, names = {"ACCOUNT_SUBJECT", "BUSINESS_PARTNER", "DEPARTMENT", "PRODUCT"})
    void distinctApprovedUpdatesRecheckVersionAfterBusinessKeyLock(MasterDataType type) throws Exception {
        verifyApprovedRace(type, ChangeType.UPDATE, ChangeType.UPDATE);
    }

    @ParameterizedTest
    @EnumSource(value = MasterDataType.class, names = {"ACCOUNT_SUBJECT", "BUSINESS_PARTNER", "DEPARTMENT", "PRODUCT"})
    void deactivateCannotApplyAnApprovalMadeBeforeCompetingUpdate(MasterDataType type) throws Exception {
        verifyApprovedRace(type, ChangeType.UPDATE, ChangeType.DEACTIVATE);
    }

    @ParameterizedTest
    @EnumSource(value = MasterDataType.class, names = {"ACCOUNT_SUBJECT", "BUSINESS_PARTNER", "DEPARTMENT", "PRODUCT"})
    void directCreateUsesSameAbsentBusinessKeyLockAsApprovedCreate(MasterDataType type) throws Exception {
        verifyDirectWinner(type, ChangeType.CREATE);
    }

    @ParameterizedTest
    @EnumSource(value = MasterDataType.class, names = {"ACCOUNT_SUBJECT", "BUSINESS_PARTNER", "DEPARTMENT", "PRODUCT"})
    void directUpdateUsesSameBusinessKeyLockAsApprovedUpdate(MasterDataType type) throws Exception {
        verifyDirectWinner(type, ChangeType.UPDATE);
    }

    @ParameterizedTest
    @EnumSource(value = MasterDataType.class, names = {"ACCOUNT_SUBJECT", "BUSINESS_PARTNER", "DEPARTMENT", "PRODUCT"})
    void approvedCreateBlocksCompetingDirectCreateUntilCommit(MasterDataType type) throws Exception {
        String key = key();
        long winner = approve(type, key, ChangeType.CREATE);
        MvcResult result = compete(() -> {
            assertThat(apply(winner).getResponse().getStatus()).isEqualTo(200);
        }, () -> mvc.perform(post("/test/direct/{type}/{key}/{change}", type, key, ChangeType.CREATE)).andReturn());
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertRequest(winner, "APPLIED", true);
        assertHistory(type, key, 1);
    }

    @ParameterizedTest
    @EnumSource(value = MasterDataType.class, names = {"ACCOUNT_SUBJECT", "BUSINESS_PARTNER", "DEPARTMENT", "PRODUCT"})
    void approvedDeactivationMakesPreviouslyApprovedUpdateConflict(MasterDataType type) throws Exception {
        verifyDeactivateBeforeUpdate(type, EFFECTIVE);
    }

    @ParameterizedTest
    @EnumSource(value = MasterDataType.class, names = {"ACCOUNT_SUBJECT", "BUSINESS_PARTNER", "DEPARTMENT", "PRODUCT"})
    void deactivationTodayMakesPreviouslyApprovedOpenEndedUpdateConflict(MasterDataType type) throws Exception {
        verifyDeactivateBeforeUpdate(type, LocalDate.now());
    }

    private void verifyDeactivateBeforeUpdate(MasterDataType type, LocalDate endDate) throws Exception {
        String key = key();
        directWrite(type, key, ChangeType.CREATE, START);
        long winner = approve(type, key, ChangeType.DEACTIVATE, endDate);
        long loser = approve(type, key, ChangeType.UPDATE);
        MvcResult result = compete(() -> {
            assertThat(apply(winner).getResponse().getStatus()).isEqualTo(200);
        }, () -> apply(loser));
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(versionReadBeforeCommit).isFalse();
        assertRequest(winner, "APPLIED", true);
        assertRequest(loser, "APPROVED", false);
        assertHistory(type, key, 1);
    }

    @ParameterizedTest
    @EnumSource(value = MasterDataType.class, names = {"ACCOUNT_SUBJECT", "BUSINESS_PARTNER", "DEPARTMENT", "PRODUCT"})
    void equalEndDeactivationsCannotBothBecomeApplied(MasterDataType type) throws Exception {
        String key = key();
        directWrite(type, key, ChangeType.CREATE, START);
        long winner = approve(type, key, ChangeType.DEACTIVATE, LocalDate.now());
        long loser = approve(type, key, ChangeType.DEACTIVATE, LocalDate.now());
        MvcResult result = compete(() -> {
            assertThat(apply(winner).getResponse().getStatus()).isEqualTo(200);
        }, () -> apply(loser));
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(versionReadBeforeCommit).isFalse();
        assertRequest(winner, "APPLIED", true);
        assertRequest(loser, "APPROVED", false);
        assertHistory(type, key, 1);
    }

    @ParameterizedTest
    @EnumSource(value = MasterDataType.class, names = {"ACCOUNT_SUBJECT", "BUSINESS_PARTNER", "DEPARTMENT", "PRODUCT"})
    void directDeactivationUsesSameLockAsApprovedDeactivation(MasterDataType type) throws Exception {
        String key = key();
        directWrite(type, key, ChangeType.CREATE, START);
        long loser = approve(type, key, ChangeType.DEACTIVATE, LocalDate.now());
        MvcResult result = compete(() -> directWrite(type, key, ChangeType.DEACTIVATE, LocalDate.now()),
                () -> apply(loser));
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(versionReadBeforeCommit).isFalse();
        assertRequest(loser, "APPROVED", false);
        assertHistory(type, key, 1);
    }

    @ParameterizedTest
    @EnumSource(value = MasterDataType.class, names = {"ACCOUNT_SUBJECT", "BUSINESS_PARTNER", "DEPARTMENT", "PRODUCT"})
    void preloadedDirectContenderCannotExtendPeriodClosedWhileWaitingForLock(MasterDataType type) throws Exception {
        String key = key();
        directWrite(type, key, ChangeType.CREATE, START);
        MvcResult result = compete(() -> directWrite(type, key, ChangeType.DEACTIVATE, LocalDate.now()),
                () -> mvc.perform(post("/test/direct-preloaded/{type}/{key}", type, key)).andReturn());
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT valid_to FROM " + table(type) + " WHERE " + codeColumn(type)
                + " = ?", LocalDate.class, key)).isEqualTo(LocalDate.now());
        assertHistory(type, key, 1);
    }

    @Test
    void firstAbsentCreateRollbackLetsWaitingCreatorOwnOneLockRow() throws Exception {
        String key = key();
        MvcResult result = compete(() -> directWrite(MasterDataType.DEPARTMENT, key, ChangeType.CREATE, START),
                () -> mvc.perform(post("/test/direct/{type}/{key}/{change}",
                        MasterDataType.DEPARTMENT, key, ChangeType.CREATE)).andReturn(), true);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertHistory(MasterDataType.DEPARTMENT, key, 1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM master_data_business_key_locks
                WHERE target_type = 'DEPARTMENT' AND target_key = ?
                """, Integer.class, key)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT valid_from FROM departments WHERE code = ?",
                LocalDate.class, key)).isEqualTo(EFFECTIVE);
    }

    @Test
    void sameRequestContenderRefreshesPreloadedApprovedStateAndAppliesOnlyOnce() throws Exception {
        String key = key();
        directWrite(MasterDataType.DEPARTMENT, key, ChangeType.CREATE, START);
        long id = approve(MasterDataType.DEPARTMENT, key, ChangeType.UPDATE);
        MvcResult result = compete(() -> assertThat(apply(id).getResponse().getStatus()).isEqualTo(200), () -> {
            requestPersistence.findById(id).orElseThrow();
            return apply(id);
        });
        assertThat(result.getResponse().getStatus()).as("resolved exception: %s", result.getResolvedException()).isEqualTo(409);
        assertRequest(id, "APPLIED", true);
        assertHistory(MasterDataType.DEPARTMENT, key, 2);
    }

    @Test
    void oneOuterTransactionCanRequestApproveAndApplyWithoutLosingPendingDecision() {
        String key = key();
        Long id = transaction.execute(status -> {
            long requested = approve(MasterDataType.DEPARTMENT, key, ChangeType.CREATE);
            changeRequests.applyApprovedChange(requested);
            return requested;
        });
        assertRequest(id, "APPLIED", true);
        assertHistory(MasterDataType.DEPARTMENT, key, 1);
    }

    @Test
    void applyDueRefreshesCandidateAppliedByConcurrentSingleRequestWithoutDeadlock() throws Exception {
        // Each run owns this schema. Earlier cases intentionally retain losing APPROVED requests;
        // defer those fixtures so the real scheduler query selects only this case's candidate.
        jdbc.update("UPDATE master_data_change_requests SET effective_date = ? WHERE status = 'APPROVED'",
                LocalDate.now().plusDays(1));
        String key = key();
        directWrite(MasterDataType.DEPARTMENT, key, ChangeType.CREATE, START);
        long id = approve(MasterDataType.DEPARTMENT, key, ChangeType.UPDATE);
        MvcResult result = compete(() -> assertThat(apply(id).getResponse().getStatus()).isEqualTo(200),
                () -> mvc.perform(post("/api/master-data/change-requests/apply-due")
                        .header("X-Auth-Roles", "ROLE_MASTER_MANAGER")).andReturn());
        assertThat(result.getResponse().getStatus()).as("resolved exception: %s", result.getResolvedException()).isEqualTo(200);
        assertThat(result.getResponse().getContentAsString()).isEqualTo("[]");
        assertRequest(id, "APPLIED", true);
        assertHistory(MasterDataType.DEPARTMENT, key, 2);
    }

    @Test
    void approvedUpdateRefreshesPreloadedCurrentWindowAfterConcurrentDeactivation() throws Exception {
        String key = key();
        directWrite(MasterDataType.DEPARTMENT, key, ChangeType.CREATE, START);
        long id = approve(MasterDataType.DEPARTMENT, key, ChangeType.UPDATE);
        MvcResult result = compete(() -> directWrite(MasterDataType.DEPARTMENT, key,
                ChangeType.DEACTIVATE, LocalDate.now()), () -> {
            departments.findDepartmentByCode(key).orElseThrow();
            return apply(id);
        });
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertRequest(id, "APPROVED", false);
        assertHistory(MasterDataType.DEPARTMENT, key, 1);
        assertThat(jdbc.queryForObject("SELECT valid_to FROM departments WHERE code = ?",
                LocalDate.class, key)).isEqualTo(LocalDate.now());
    }

    @ParameterizedTest
    @EnumSource(value = MasterDataType.class, names = {"ACCOUNT_SUBJECT", "BUSINESS_PARTNER", "DEPARTMENT", "PRODUCT"})
    void rollbackRestoresScd2HistoryAndApprovedStateBeforeRetry(MasterDataType type) {
        String key = key();
        directWrite(type, key, ChangeType.CREATE, START);
        long request = approve(type, key, ChangeType.UPDATE);
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            changeRequests.applyApprovedChange(request);
            entityManager.flush();
            throw new RollbackProbe();
        })).isInstanceOf(RollbackProbe.class);

        assertRequest(request, "APPROVED", false);
        assertHistory(type, key, 1);
        assertThat(jdbc.queryForObject("SELECT valid_to FROM " + table(type) + " WHERE "
                + codeColumn(type) + " = ?", LocalDate.class, key)).isEqualTo(END);
        changeRequests.applyApprovedChange(request);
        assertRequest(request, "APPLIED", true);
        assertHistory(type, key, 2);
    }

    @Test
    void unrelatedKeysAndDifferentTargetTypesProceedWhileOneKeyIsLocked() throws Exception {
        String key = key();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch written = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<?> held = pool.submit(() -> transaction.executeWithoutResult(status -> {
            directWrite(MasterDataType.DEPARTMENT, key, ChangeType.CREATE, START);
            entityManager.flush();
            written.countDown();
            await(release);
        }));
        try {
            assertThat(written.await(10, TimeUnit.SECONDS)).isTrue();
            Future<?> independent = pool.submit(() -> {
                directWrite(MasterDataType.DEPARTMENT, key(), ChangeType.CREATE, START);
                directWrite(MasterDataType.ACCOUNT_SUBJECT, key, ChangeType.CREATE, START);
            });
            independent.get(5, TimeUnit.SECONDS);
            assertThat(held.isDone()).isFalse();
        } finally {
            release.countDown();
            held.get(10, TimeUnit.SECONDS);
            pool.shutdownNow();
        }
    }

    @ParameterizedTest
    @EnumSource(value = MasterDataType.class, names = {"ACCOUNT_SUBJECT", "BUSINESS_PARTNER", "DEPARTMENT", "PRODUCT"})
    void databaseRejectsOverlapAndReversedPeriodsButAllowsAdjacentHistory(MasterDataType type) throws Exception {
        String key = key();
        directWrite(type, key, ChangeType.CREATE, START);
        assertThat(probe(type, key, "overlap").getResponse().getStatus()).isEqualTo(409);
        assertThat(probe(type, key, "reversed").getResponse().getStatus()).isEqualTo(409);
        directWrite(type, key, ChangeType.UPDATE, EFFECTIVE);
        assertHistory(type, key, 2);
    }

    @ParameterizedTest
    @EnumSource(value = MasterDataType.class, names = {"ACCOUNT_SUBJECT", "BUSINESS_PARTNER", "DEPARTMENT", "PRODUCT"})
    void unrelatedNotNullViolationIsNotReportedAsBusinessConflict(MasterDataType type) throws Exception {
        String key = key();
        directWrite(type, key, ChangeType.CREATE, START);
        MvcResult result = probe(type, key, "unrelated");
        assertThat(result.getResponse().getStatus()).isEqualTo(500);
        assertThat(result.getResponse().getContentAsString()).doesNotContain(key, table(type), "null value", "SQL");
        assertHistory(type, key, 1);
    }

    @Test
    void unrelatedCheckAndExclusionSqlStatesRemainInternalErrors() throws Exception {
        jdbc.execute("""
                CREATE TABLE synthetic_constraint_probe (
                    amount integer CONSTRAINT ck_probe_positive CHECK (amount > 0),
                    valid_period daterange,
                    CONSTRAINT ex_probe_period EXCLUDE USING gist (valid_period WITH &&)
                )
                """);
        jdbc.update("INSERT INTO synthetic_constraint_probe VALUES (1, daterange('2025-01-01', '2025-01-31', '[]'))");
        for (String operation : List.of("check", "exclusion")) {
            MvcResult response = mvc.perform(post("/test/unrelated/{operation}", operation)).andReturn();
            assertThat(sqlState(response.getResolvedException())).isEqualTo(operation.equals("check") ? "23514" : "23P01");
            assertThat(response.getResponse().getStatus()).isEqualTo(500);
            assertThat(response.getResponse().getContentAsString())
                    .doesNotContain("synthetic_constraint_probe", "ck_probe_positive", "ex_probe_period");
        }
    }

    private void verifyApprovedRace(MasterDataType type, ChangeType first, ChangeType second) throws Exception {
        String key = key();
        if (first != ChangeType.CREATE) {
            directWrite(type, key, ChangeType.CREATE, START);
        }
        long winner = approve(type, key, first);
        long loser = approve(type, key, second);
        assertThat(winner).isNotEqualTo(loser);
        MvcResult result = compete(() -> {
            assertThat(apply(winner).getResponse().getStatus()).isEqualTo(200);
        }, () -> apply(loser));
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(versionReadBeforeCommit).as("requestedVersion is checked only after the winner commits").isFalse();
        assertRequest(winner, "APPLIED", true);
        assertRequest(loser, "APPROVED", false);
        assertHistory(type, key, first == ChangeType.CREATE ? 1 : 2);
    }

    private void verifyDirectWinner(MasterDataType type, ChangeType change) throws Exception {
        String key = key();
        if (change == ChangeType.UPDATE) {
            directWrite(type, key, ChangeType.CREATE, START);
        }
        long loser = approve(type, key, change);
        MvcResult result = compete(() -> directWrite(type, key, change, EFFECTIVE), () -> apply(loser));
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(versionReadBeforeCommit).as("direct writes retain the same key lock until commit").isFalse();
        assertRequest(loser, "APPROVED", false);
        assertHistory(type, key, change == ChangeType.CREATE ? 1 : 2);
    }

    private MvcResult compete(CheckedRunnable first, CheckedSupplier<MvcResult> second) throws Exception {
        return compete(first, second, false);
    }

    private MvcResult compete(CheckedRunnable first, CheckedSupplier<MvcResult> second, boolean rollbackWinner) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch written = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch competingStarted = new CountDownLatch(1);
        AtomicInteger backend = new AtomicInteger();
        Future<?> winner = pool.submit(() -> transaction.executeWithoutResult(status -> {
            unchecked(first);
            entityManager.flush();
            ownerUncommitted.set(true);
            written.countDown();
            await(release);
            if (rollbackWinner) status.setRollbackOnly();
        }));
        Future<MvcResult> loser = null;
        try {
            assertThat(written.await(10, TimeUnit.SECONDS)).as("winning write reaches its commit barrier").isTrue();
            loser = pool.submit(() -> {
                AtomicReference<MvcResult> response = new AtomicReference<>();
                try {
                    return transaction.execute(status -> {
                        competingThread.set(Thread.currentThread());
                        backend.set(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
                        competingStarted.countDown();
                        response.set(unchecked(second));
                        return response.get();
                    });
                } catch (UnexpectedRollbackException exception) {
                    // HTTP advice handles the service exception before the enclosing observer
                    // transaction exits. Retain that real response after Spring rolls back the write.
                    if (response.get() == null || response.get().getResponse().getStatus() < 400) throw exception;
                    return response.get();
                }
            });
            assertThat(competingStarted.await(10, TimeUnit.SECONDS)).isTrue();
            awaitDatabaseBlockOrCompletion(loser, backend.get());
            // This gate is based on PostgreSQL's actual blocking graph, not a timing guess. Baseline
            // CREATE may finish without blocking; UPDATE may block after the stale version query.
            ownerUncommitted.set(false);
            release.countDown();
            winner.get(10, TimeUnit.SECONDS);
            return loser.get(10, TimeUnit.SECONDS);
        } finally {
            ownerUncommitted.set(false);
            release.countDown();
            pool.shutdownNow();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void awaitDatabaseBlockOrCompletion(Future<?> competing, int backend) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (!competing.isDone() && System.nanoTime() < deadline) {
            if (Boolean.TRUE.equals(jdbc.queryForObject(
                    "SELECT cardinality(pg_blocking_pids(?)) > 0", Boolean.class, backend))) {
                return;
            }
            Thread.sleep(10);
        }
        assertThat(competing.isDone()).as("competitor either blocks in PostgreSQL or completes").isTrue();
    }

    private long approve(MasterDataType type, String key, ChangeType change) {
        return approve(type, key, change, EFFECTIVE);
    }

    private long approve(MasterDataType type, String key, ChangeType change, LocalDate effectiveDate) {
        int version = change == ChangeType.UPDATE ? 2 : 1;
        String payload = change == ChangeType.DEACTIVATE ? null : payload(type, key);
        MasterDataChangeRequest request = changeRequests.requestChange(new MasterDataChangeRequestCommand(
                type, key, change, effectiveDate, version, "synthetic-requester", "concurrency regression", payload));
        return changeRequests.approve(request.getId(), "synthetic-approver").getId();
    }

    private MvcResult apply(long id) throws Exception {
        return mvc.perform(post("/api/master-data/change-requests/{id}/apply", id)
                .header("X-Auth-Roles", "ROLE_MASTER_MANAGER")).andReturn();
    }

    private MvcResult probe(MasterDataType type, String key, String operation) throws Exception {
        return mvc.perform(post("/test/scd2/{type}/{key}/{operation}", type, key, operation)).andReturn();
    }

    private void directWrite(MasterDataType type, String key, ChangeType change, LocalDate from) {
        if (change == ChangeType.DEACTIVATE) {
            switch (type) {
                case ACCOUNT_SUBJECT -> accounts.deactivateAccountSubject(key, from);
                case BUSINESS_PARTNER -> partners.deleteBusinessPartner(
                        partners.getBusinessPartnerByCode(key).orElseThrow().getId(), from);
                case DEPARTMENT -> departments.deactivateDepartment(key, from);
                case PRODUCT -> products.deactivateProduct(products.getProductByProductCode(key).orElseThrow().getId(), from);
                default -> throw new IllegalArgumentException("Unsupported fixture type");
            }
            return;
        }
        switch (type) {
            case ACCOUNT_SUBJECT -> {
                AccountSubjectCommand command = new AccountSubjectCommand(key, "Synthetic account", null,
                        AccountSubject.AccountCategory.ASSETS, AccountSubject.BalanceType.DEBIT, null,
                        false, false, from, END);
                if (change == ChangeType.CREATE) accounts.createAccountSubject(command);
                else accounts.updateAccountSubject(key, command);
            }
            case BUSINESS_PARTNER -> {
                BusinessPartnerCommand command = new BusinessPartnerCommand(key, "Synthetic partner", null,
                        null, null, null, BusinessPartner.PartnerType.VENDOR, true,
                        BusinessPartner.KycStatus.APPROVED, BusinessPartner.RiskRating.LOW, from, END);
                if (change == ChangeType.CREATE) partners.createBusinessPartner(command);
                else partners.updateBusinessPartner(partners.getBusinessPartnerByCode(key).orElseThrow().getId(), command);
            }
            case DEPARTMENT -> {
                DepartmentCommand command = new DepartmentCommand(key, "Synthetic department", null,
                        Department.DepartmentType.COST_CENTER, from, END);
                if (change == ChangeType.CREATE) departments.createDepartment(command);
                else departments.updateDepartment(key, command);
            }
            case PRODUCT -> {
                ProductCommand command = new ProductCommand(key, "Synthetic product", null, "EA",
                        BigDecimal.ONE, Product.ProductType.PHYSICAL, from, END);
                if (change == ChangeType.CREATE) products.createProduct(command);
                else products.updateProduct(products.getProductByProductCode(key).orElseThrow().getId(), command);
            }
            default -> throw new IllegalArgumentException("Unsupported fixture type");
        }
    }

    private void assertRequest(long id, String status, boolean appliedAt) {
        assertThat(jdbc.queryForObject("SELECT status FROM master_data_change_requests WHERE id = ?",
                String.class, id)).isEqualTo(status);
        assertThat(jdbc.queryForObject("SELECT applied_at IS NOT NULL FROM master_data_change_requests WHERE id = ?",
                Boolean.class, id)).isEqualTo(appliedAt);
    }

    private void assertHistory(MasterDataType type, String key, int count) {
        String table = table(type);
        String column = codeColumn(type);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE " + column + " = ?",
                Integer.class, key)).isEqualTo(count);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table + " a JOIN " + table
                        + " b ON a." + column + " = b." + column + " AND a.id < b.id"
                        + " AND a.valid_from <= b.valid_to AND b.valid_from <= a.valid_to"
                        + " WHERE a." + column + " = ?", Integer.class, key)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE " + column
                + " = ? AND valid_from > valid_to", Integer.class, key)).isZero();
    }

    private static String payload(MasterDataType type, String key) {
        return switch (type) {
            case ACCOUNT_SUBJECT -> "{\"code\":\"" + key
                    + "\",\"name\":\"Synthetic account\",\"category\":\"ASSETS\",\"balanceType\":\"DEBIT\"}";
            case BUSINESS_PARTNER -> "{\"businessPartnerCode\":\"" + key
                    + "\",\"businessPartnerName\":\"Synthetic partner\",\"partnerType\":\"VENDOR\"}";
            case DEPARTMENT -> "{\"code\":\"" + key + "\",\"name\":\"Synthetic department\"}";
            case PRODUCT -> "{\"productCode\":\"" + key
                    + "\",\"name\":\"Synthetic product\",\"productType\":\"PHYSICAL\",\"price\":1}";
            default -> throw new IllegalArgumentException("Unsupported fixture type");
        };
    }

    private static String table(MasterDataType type) {
        return switch (type) {
            case ACCOUNT_SUBJECT -> "account_subjects";
            case BUSINESS_PARTNER -> "business_partners";
            case DEPARTMENT -> "departments";
            case PRODUCT -> "products";
            default -> throw new IllegalArgumentException("Unsupported fixture type");
        };
    }

    private static String codeColumn(MasterDataType type) {
        return switch (type) {
            case ACCOUNT_SUBJECT, DEPARTMENT -> "code";
            case BUSINESS_PARTNER -> "business_partner_code";
            case PRODUCT -> "product_code";
            default -> throw new IllegalArgumentException("Unsupported fixture type");
        };
    }

    private static String key() {
        return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 15);
    }

    private static String sqlState(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException) return sqlException.getSQLState();
        }
        return null;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) throw new AssertionError("Transaction barrier timed out");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Transaction barrier interrupted", exception);
        }
    }

    private static void unchecked(CheckedRunnable action) {
        unchecked(() -> { action.run(); return null; });
    }

    private static <T> T unchecked(CheckedSupplier<T> action) {
        try {
            return action.get();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @FunctionalInterface private interface CheckedRunnable { void run() throws Exception; }
    @FunctionalInterface private interface CheckedSupplier<T> { T get() throws Exception; }
    private static class RollbackProbe extends RuntimeException { }

    @RestController
    class ConstraintProbeController {
        @PostMapping("/test/direct-preloaded/{type}/{key}")
        void preloaded(@PathVariable MasterDataType type, @PathVariable String key) {
            // Load before the direct write tries its lock. A post-lock repository query must refresh
            // this transaction's existing JPA entity, not return its previously cached open interval.
            switch (type) {
                case ACCOUNT_SUBJECT -> accounts.findAccountSubjectByCode(key).orElseThrow();
                case BUSINESS_PARTNER -> partners.getBusinessPartnerByCode(key).orElseThrow();
                case DEPARTMENT -> departments.findDepartmentByCode(key).orElseThrow();
                case PRODUCT -> products.getProductByProductCode(key).orElseThrow();
                default -> throw new IllegalArgumentException("Unsupported fixture type");
            }
            directWrite(type, key, ChangeType.DEACTIVATE, LocalDate.now().plusDays(1));
        }

        @PostMapping("/test/unrelated/{operation}")
        void unrelated(@PathVariable String operation) {
            if (operation.equals("check")) {
                jdbc.update("INSERT INTO synthetic_constraint_probe VALUES (-1, daterange('2025-03-01', '2025-03-31', '[]'))");
            } else {
                jdbc.update("INSERT INTO synthetic_constraint_probe VALUES (1, daterange('2025-01-31', '2025-02-01', '[]'))");
            }
        }

        @PostMapping("/test/direct/{type}/{key}/{change}")
        void direct(@PathVariable MasterDataType type, @PathVariable String key, @PathVariable ChangeType change) {
            directWrite(type, key, change, EFFECTIVE);
        }

        @PostMapping("/test/scd2/{type}/{key}/{operation}")
        void write(@PathVariable MasterDataType type, @PathVariable String key, @PathVariable String operation) {
            String table = table(type);
            String code = codeColumn(type);
            switch (operation) {
                case "overlap" -> {
                    List<String> columns = jdbc.queryForList("""
                            SELECT column_name FROM information_schema.columns
                            WHERE table_schema = ? AND table_name = ? AND column_name <> 'id'
                            ORDER BY ordinal_position
                            """, String.class, SCHEMA, table);
                    String joined = String.join(", ", columns);
                    jdbc.update("INSERT INTO " + table + " (" + joined + ") SELECT " + joined
                            + " FROM " + table + " WHERE " + code + " = ?", key);
                }
                case "reversed" -> jdbc.update("UPDATE " + table + " SET valid_to = valid_from - 1 WHERE "
                        + code + " = ?", key);
                case "unrelated" -> jdbc.update("UPDATE " + table + " SET "
                        + (type == MasterDataType.BUSINESS_PARTNER ? "business_partner_name" : "name")
                        + " = NULL WHERE " + code + " = ?", key);
                default -> throw new IllegalArgumentException("Unknown fixture operation");
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @ComponentScan(basePackages = {"com.ho.account.masterdata.core.application", "com.ho.account.masterdata.core.infrastructure"})
    @EntityScan(basePackages = {"com.ho.account.masterdata.core.domain.model",
            "com.ho.account.masterdata.core.domain.changerequest", "com.ho.account.masterdata.core.infrastructure.persistence"})
    @EnableJpaRepositories(basePackages = "com.ho.account.masterdata.core.infrastructure.persistence")
    static class TestApplication { }
}
