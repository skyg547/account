package com.ho.account.masterdata.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.ho.account.masterdata.core.application.port.out.MasterDataBusinessKeyLockPort;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangeRequestPersistencePort;
import com.ho.account.masterdata.core.application.port.out.MasterDataVersionQueryPort;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.infrastructure.persistence.JpaMasterDataChangeRequestPersistenceAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.MasterDataChangeRequestMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Committed H2 rows expose failures that an ambient test rollback would hide. */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.datasource.hikari.maximum-pool-size=6"
})
@ContextConfiguration(classes = MasterDataDueChangeIsolationTest.TestApplication.class)
@Import({MasterDataChangeRequestService.class, MasterDataDueChangeExecutor.class,
        JpaMasterDataChangeRequestPersistenceAdapter.class,
        MasterDataChangeRequestMapper.class, MasterDataDueChangeIsolationTest.TestDependencies.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MasterDataDueChangeIsolationTest {

    private static final LocalDate DUE = LocalDate.now().minusDays(1);

    @Autowired private MasterDataChangeRequestService changes;
    @Autowired private JpaMasterDataChangeRequestPersistenceAdapter requests;
    @Autowired private MasterDataChangeApplier applier;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private DataSource dataSource;

    private final Map<String, AtomicInteger> applies = new ConcurrentHashMap<>();
    private TransactionTemplate transaction;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.ho.account.masterdata.core")
    @EnableJpaRepositories(basePackages = "com.ho.account.masterdata.core.infrastructure.persistence")
    @Import({MasterDataChangeRequestService.class, MasterDataDueChangeExecutor.class,
            JpaMasterDataChangeRequestPersistenceAdapter.class,
            MasterDataChangeRequestMapper.class, TestDependencies.class})
    static class TestApplication { }

    @TestConfiguration
    static class TestDependencies {
        @Bean MasterDataChangeApplier testApplier() {
            MasterDataChangeApplier applier = mock(MasterDataChangeApplier.class);
            when(applier.targetType()).thenReturn(MasterDataType.DEPARTMENT);
            return applier;
        }

        @Bean MasterDataVersionQueryPort versions() {
            return (type, key) -> 0;
        }

        @Bean MasterDataBusinessKeyLockPort locks() {
            return (type, key) -> { };
        }
    }

    @BeforeEach
    void resetCommittedRowsAndApplier() {
        transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> entityManager.createNativeQuery(
                "delete from master_data_change_requests").executeUpdate());
        applies.clear();
        reset(applier);
        when(applier.targetType()).thenReturn(MasterDataType.DEPARTMENT);
        doAnswer(invocation -> {
            MasterDataChangeRequest request = invocation.getArgument(0);
            if (request.getTargetKey().equals("B")) {
                // A write performed before the applier fails must roll back with that request.
                entityManager.createNativeQuery(
                        "update master_data_change_requests set reason = 'transient write' where id = :id")
                        .setParameter("id", request.getId()).executeUpdate();
                throw new IllegalArgumentException("invalid B payload");
            }
            applies.computeIfAbsent(request.getTargetKey(), ignored -> new AtomicInteger()).incrementAndGet();
            return null;
        }).when(applier).apply(any());
    }

    @Test
    void badMiddleRequestIsRecordedWhileNeighborsCommitAndRestartDoesNotReplayThem() {
        Long a = approved("A", DUE);
        Long b = approved("B", DUE);
        Long c = approved("C", DUE);

        List<MasterDataChangeRequest> first = changes.applyDueApprovedChanges();

        assertThat(first).extracting(MasterDataChangeRequest::getId).containsExactly(a, b, c);
        assertStatus(a, "APPLIED");
        assertStatus(b, "FAILED_INVALID");
        assertStatus(c, "APPLIED");
        MasterDataChangeRequest failed = requests.findById(b).orElseThrow();
        assertThat(failed.getAppliedAt()).isNull();
        assertThat(failed.getFailureReason()).isEqualTo("INVALID_REQUEST");
        assertThat(failed.getFailedAt()).isNotNull();
        assertThat(failed.getReason()).isEqualTo("due isolation");

        // A second Spring context has fresh service and EntityManager beans but reads the same
        // committed H2 database. It simulates restart without relying on first-level JPA cache.
        try (ConfigurableApplicationContext restarted = restartAgainstSameDatabase()) {
            assertThat(restarted.getBean(MasterDataChangeRequestService.class)).isNotSameAs(changes);
            assertThat(restarted.getBean(EntityManagerFactory.class)).isNotSameAs(entityManagerFactory);
            MasterDataChangeRequestPersistencePort restoredRows =
                    restarted.getBean(MasterDataChangeRequestPersistencePort.class);
            assertThat(restoredRows.findById(a).orElseThrow().getStatus().name()).isEqualTo("APPLIED");
            assertThat(restoredRows.findById(b).orElseThrow().getStatus().name()).isEqualTo("FAILED_INVALID");
            assertThat(restoredRows.findById(b).orElseThrow().getFailureReason())
                    .isEqualTo("INVALID_REQUEST");
            assertThat(restoredRows.findById(c).orElseThrow().getStatus().name()).isEqualTo("APPLIED");
            assertThat(restarted.getBean(MasterDataChangeRequestService.class)
                    .applyDueApprovedChanges()).isEmpty();
            org.mockito.Mockito.verify(restarted.getBean(MasterDataChangeApplier.class),
                    org.mockito.Mockito.never()).apply(any());
        }
        assertThat(applies.get("A").get()).isEqualTo(1);
        assertThat(applies.get("C").get()).isEqualTo(1);
    }

    @Test
    void transientDatabaseFailureLeavesCurrentAndLaterWorkRetryableAcrossRestart() {
        Long a = approved("A", DUE);
        Long b = approved("B", DUE);
        Long c = approved("C", DUE);
        reset(applier);
        when(applier.targetType()).thenReturn(MasterDataType.DEPARTMENT);
        doAnswer(invocation -> {
            MasterDataChangeRequest request = invocation.getArgument(0);
            if (request.getTargetKey().equals("B")) {
                entityManager.createNativeQuery(
                        "update master_data_change_requests set reason = 'transient write' where id = :id")
                        .setParameter("id", request.getId()).executeUpdate();
                throw new TransientDataAccessResourceException("synthetic temporary database outage");
            }
            applies.computeIfAbsent(request.getTargetKey(), ignored -> new AtomicInteger()).incrementAndGet();
            return null;
        }).when(applier).apply(any());

        assertThatThrownBy(() -> changes.applyDueApprovedChanges())
                .isInstanceOf(TransientDataAccessResourceException.class);
        assertStatus(a, "APPLIED");
        assertStatus(b, "APPROVED");
        assertStatus(c, "APPROVED");
        assertThat(requests.findById(b).orElseThrow().getReason()).isEqualTo("due isolation");
        assertThat(requests.findById(b).orElseThrow().getFailureReason()).isNull();
        assertThat(applies.get("A").get()).isEqualTo(1);

        try (ConfigurableApplicationContext restarted = restartAgainstSameDatabase()) {
            MasterDataChangeApplier recovered = restarted.getBean(MasterDataChangeApplier.class);
            doAnswer(invocation -> {
                MasterDataChangeRequest request = invocation.getArgument(0);
                applies.computeIfAbsent(request.getTargetKey(), ignored -> new AtomicInteger())
                        .incrementAndGet();
                return null;
            }).when(recovered).apply(any());
            assertThat(restarted.getBean(MasterDataChangeRequestService.class)
                    .applyDueApprovedChanges())
                    .extracting(MasterDataChangeRequest::getId)
                    .containsExactly(b, c);
            MasterDataChangeRequestPersistencePort recoveredRows =
                    restarted.getBean(MasterDataChangeRequestPersistencePort.class);
            assertThat(recoveredRows.findById(a).orElseThrow().getStatus().name()).isEqualTo("APPLIED");
            assertThat(recoveredRows.findById(b).orElseThrow().getStatus().name()).isEqualTo("APPLIED");
            assertThat(recoveredRows.findById(c).orElseThrow().getStatus().name()).isEqualTo("APPLIED");
        }
        assertThat(applies.get("A").get()).isEqualTo(1);
        assertThat(applies.get("B").get()).isEqualTo(1);
        assertThat(applies.get("C").get()).isEqualTo(1);
    }

    @Test
    void failureRecordWriteErrorPropagatesWithoutReportingAFalseFailedResult() {
        MasterDataChangeRequest candidate = MasterDataChangeRequest.reconstitute(
                998L, MasterDataType.DEPARTMENT, "BAD", ChangeType.CREATE,
                ChangeStatus.APPROVED, 0L, DUE, 1, "maker", "checker",
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(2),
                "due isolation", "{\"name\":\"synthetic\"}", null, null);
        MasterDataChangeRequestPersistencePort selection = mock(MasterDataChangeRequestPersistencePort.class);
        MasterDataDueChangeExecutor executor = mock(MasterDataDueChangeExecutor.class);
        when(selection.findReadyToApply(LocalDate.now(), 500)).thenReturn(List.of(candidate));
        when(executor.applyDue(998L, LocalDate.now()))
                .thenThrow(new IllegalArgumentException("synthetic invalid request"));
        when(executor.recordFailure(998L, LocalDate.now(),
                ChangeStatus.FAILED_INVALID, "INVALID_REQUEST"))
                .thenThrow(new TransientDataAccessResourceException("synthetic failure-record outage"));
        MasterDataChangeRequestService isolated = new MasterDataChangeRequestService(
                selection, (type, key) -> 0, List.of(applier), (type, key) -> { }, executor);

        assertThatThrownBy(isolated::applyDueApprovedChanges)
                .isInstanceOf(TransientDataAccessResourceException.class);
        assertThat(candidate.getStatus()).isEqualTo(ChangeStatus.APPROVED);
        assertThat(candidate.getAppliedAt()).isNull();
        assertThat(candidate.getFailureReason()).isNull();
    }

    @Test
    void twoWorkersDoNotApplyTheSameApprovedRequestTwice() throws Exception {
        Long id = approved("A", DUE);
        CountDownLatch start = new CountDownLatch(1);
        var workers = Executors.newFixedThreadPool(2);
        try {
            Callable<List<MasterDataChangeRequest>> run = () -> {
                start.await(5, TimeUnit.SECONDS);
                return changes.applyDueApprovedChanges();
            };
            var first = workers.submit(run);
            var second = workers.submit(run);
            start.countDown();
            assertThat(first.get(15, TimeUnit.SECONDS)).hasSizeLessThanOrEqualTo(1);
            assertThat(second.get(15, TimeUnit.SECONDS)).hasSizeLessThanOrEqualTo(1);
        } finally {
            workers.shutdownNow();
        }
        assertStatus(id, "APPLIED");
        assertThat(applies.get("A").get()).isEqualTo(1);
    }

    @Test
    void earliestFiveHundredAreSelectedBeforeLaterDueRequest() {
        Long later = approved("LATER", DUE);
        transaction.executeWithoutResult(status -> entityManager.createNativeQuery(
                "update master_data_change_requests set effective_date = :later where id = :id")
                .setParameter("later", DUE.plusDays(1)).setParameter("id", later).executeUpdate());
        List<Long> expectedFirstPage = new ArrayList<>();
        for (int index = 0; index < 500; index++) {
            expectedFirstPage.add(approved("N" + index, DUE));
        }

        List<MasterDataChangeRequest> first = changes.applyDueApprovedChanges();

        assertThat(first).hasSize(500);
        assertThat(first).extracting(MasterDataChangeRequest::getId)
                .containsExactlyElementsOf(expectedFirstPage);
        assertStatus(later, "APPROVED");
        assertThat(changes.applyDueApprovedChanges()).extracting(MasterDataChangeRequest::getId)
                .containsExactly(later);
    }

    @Test
    void selectionOrdersByEffectiveDateThenRequestedAtThenIdForTies() {
        Long newestId = approved("NEWEST", DUE);
        Long earlierDate = approved("EARLIER-DATE", DUE.minusDays(1));
        Long tieOne = approved("TIE-ONE", DUE);
        Long tieTwo = approved("TIE-TWO", DUE);
        LocalDateTime sameRequestedAt = LocalDateTime.of(2026, 1, 1, 12, 0);
        transaction.executeWithoutResult(status -> {
            entityManager.createNativeQuery(
                    "update master_data_change_requests set requested_at = :time where id in (:first, :second)")
                    .setParameter("time", sameRequestedAt)
                    .setParameter("first", tieOne)
                    .setParameter("second", tieTwo)
                    .executeUpdate();
        });

        assertThat(requests.findReadyToApply(LocalDate.now(), 500))
                .extracting(MasterDataChangeRequest::getId)
                .containsExactly(earlierDate, tieOne, tieTwo, newestId);
    }

    private Long approved(String key, LocalDate effectiveDate) {
        return transaction.execute(status -> {
            MasterDataChangeRequest request = new MasterDataChangeRequest(
                    MasterDataType.DEPARTMENT, key, ChangeType.CREATE, effectiveDate,
                    1, "maker", "due isolation", "{\"name\":\"synthetic\"}");
            request.approve("checker");
            return requests.save(request).getId();
        });
    }

    private void assertStatus(Long id, String expected) {
        assertThat(requests.findById(id).orElseThrow().getStatus().name()).isEqualTo(expected);
    }

    private ConfigurableApplicationContext restartAgainstSameDatabase() {
        String jdbcUrl;
        try (var connection = dataSource.getConnection()) {
            jdbcUrl = connection.getMetaData().getURL();
        } catch (java.sql.SQLException failure) {
            throw new AssertionError("Unable to read synthetic H2 JDBC URL", failure);
        }
        return new SpringApplicationBuilder(TestApplication.class)
                .web(WebApplicationType.NONE)
                .properties(Map.of(
                        "spring.datasource.url", jdbcUrl,
                        "spring.datasource.driver-class-name", "org.h2.Driver",
                        "spring.datasource.username", "sa",
                        "spring.datasource.password", "",
                        "spring.jpa.hibernate.ddl-auto", "none",
                        "spring.flyway.enabled", "false"))
                .properties("spring.cloud.vault.enabled=false",
                        "spring.cloud.config.enabled=false",
                        "spring.cloud.config.import-check.enabled=false",
                        "spring.cloud.discovery.enabled=false",
                        "eureka.client.enabled=false")
                .run();
    }

}
