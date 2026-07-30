package com.ho.account.budget.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.budget.application.exception.BudgetConflictException;
import com.ho.account.budget.application.exception.BudgetRuleViolationException;
import com.ho.account.budget.application.port.in.ExecuteBudgetCommand;
import com.ho.account.budget.application.port.in.RequestBudgetTransferCommand;
import com.ho.account.budget.application.service.BudgetManagementService;
import com.ho.account.budget.domain.BudgetExecution;
import com.ho.account.budget.domain.BudgetFiscalYearStatus;
import com.ho.account.budget.domain.BudgetPlan;
import com.ho.account.budget.domain.BudgetPlanStatus;
import com.ho.account.budget.domain.BudgetTransfer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.datasource.hikari.maximum-pool-size=6"
})
@ContextConfiguration(classes = BudgetConcurrencyIntegrationTest.TestApplication.class)
@Sql(scripts = "/db/migration/V50__create_budget_control_tables.sql",
        executionPhase = ExecutionPhase.BEFORE_TEST_CLASS)
@Import({
    BudgetManagementService.class,
    JpaBudgetPlanPersistenceAdapter.class,
    JpaBudgetTransferPersistenceAdapter.class,
    JpaBudgetExecutionPersistenceAdapter.class,
    JpaBudgetFiscalYearControlPersistenceAdapter.class,
    JpaBudgetIdempotencyLockAdapter.class,
    BudgetPlanPersistenceMapper.class,
    BudgetTransferPersistenceMapper.class,
    BudgetExecutionPersistenceMapper.class,
    BudgetFiscalYearControlPersistenceMapper.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BudgetConcurrencyIntegrationTest {

    @Autowired
    private BudgetManagementService service;

    @Autowired
    private JpaBudgetPlanPersistenceAdapter planAdapter;

    @Autowired
    private JpaBudgetFiscalYearControlPersistenceAdapter fiscalAdapter;

    @Autowired
    private SpringDataBudgetTransferRepository transferRepository;

    @Autowired
    private SpringDataBudgetExecutionRepository executionRepository;

    @Autowired
    private SpringDataBudgetPlanRepository planRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ExecutorService executor;
    private Long sourcePlanId;
    private Long targetPlanId;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = BudgetPlanJpaEntity.class)
    @EnableJpaRepositories(basePackageClasses = SpringDataBudgetPlanRepository.class)
    static class TestApplication {
    }

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            executionRepository.deleteAllInBatch();
            transferRepository.deleteAllInBatch();
            planRepository.deleteAllInBatch();
            jdbcTemplate.update("""
                    update budget_fiscal_year_controls
                    set status = 'OPEN', closed_by = null, closed_at = null
                    where fiscal_year = '2026'
                    """);
            sourcePlanId = saveApproved("SOURCE", "D001").id();
            targetPlanId = saveApproved("TARGET", "D002").id();
        });
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void firstConcurrentIdenticalTransferRequestsReturnOnePersistedResult() throws Exception {
        RequestBudgetTransferCommand command = new RequestBudgetTransferCommand(
                "REQ-CONCURRENT", sourcePlanId, targetPlanId, new BigDecimal("10.00"), "maker");

        List<BudgetTransfer> results = runTogether(
                () -> service.requestTransfer(command),
                () -> service.requestTransfer(command));

        assertThat(results).extracting(BudgetTransfer::id).doesNotContainNull().containsOnly(results.get(0).id());
        assertThat(transferRepository.findByRequestKey("REQ-CONCURRENT")).isPresent();
        assertThat(transferRepository.count()).isEqualTo(1);
    }

    @Test
    void firstConcurrentIdenticalExecutionsChargePlanAndPersistExactlyOnce() throws Exception {
        ExecuteBudgetCommand command = new ExecuteBudgetCommand(
                sourcePlanId,
                "AP",
                "INV-CONCURRENT",
                "LINE-1",
                LocalDate.of(2026, 1, 31),
                new BigDecimal("10.00"),
                "executor");

        List<BudgetExecution> results = runTogether(
                () -> service.execute(command),
                () -> service.execute(command));

        assertThat(results).extracting(BudgetExecution::id).doesNotContainNull().containsOnly(results.get(0).id());
        assertThat(executionRepository.count()).isEqualTo(1);
        BudgetPlan plan = new TransactionTemplate(transactionManager)
                .execute(status -> planAdapter.findById(sourcePlanId).orElseThrow());
        assertThat(plan.executedAmount()).isEqualByComparingTo("10.00");
    }

    @Test
    void firstConcurrentConflictingTransferPayloadProducesTypedConflict() throws Exception {
        RequestBudgetTransferCommand first = new RequestBudgetTransferCommand(
                "REQ-CONFLICT", sourcePlanId, targetPlanId, new BigDecimal("10.00"), "maker");
        RequestBudgetTransferCommand second = new RequestBudgetTransferCommand(
                "REQ-CONFLICT", sourcePlanId, targetPlanId, new BigDecimal("11.00"), "maker");

        List<Object> outcomes = runTogetherCapturing(
                () -> service.requestTransfer(first),
                () -> service.requestTransfer(second));

        assertThat(outcomes).filteredOn(BudgetTransfer.class::isInstance).hasSize(1);
        assertThat(outcomes).filteredOn(BudgetConflictException.class::isInstance).hasSize(1);
        assertThat(transferRepository.count()).isEqualTo(1);
    }

    @Test
    void fiscalCloseAndApprovalSerializeOnControlBeforePlanLocks() throws Exception {
        Long draftId = new TransactionTemplate(transactionManager).execute(status ->
                planAdapter.save(BudgetPlan.create(
                                "DRAFT-RACE",
                                "202601",
                                "D003",
                                "A100",
                                new BigDecimal("100.00"),
                                "maker"))
                        .id());

        List<Object> outcomes = runTogetherCapturing(
                () -> service.closeFiscalYear("2026", "closer"),
                () -> service.approvePlan(draftId, "checker"));

        assertThat(outcomes).filteredOn(Integer.class::isInstance).hasSize(1);
        assertThat(outcomes)
                .filteredOn(value -> value instanceof BudgetPlan
                        || value instanceof BudgetRuleViolationException)
                .hasSize(1);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertThat(fiscalAdapter.findByFiscalYearForUpdate("2026").orElseThrow().status())
                    .isEqualTo(BudgetFiscalYearStatus.CLOSED);
            BudgetPlan reloaded = planAdapter.findById(draftId).orElseThrow();
            assertThat(reloaded.status()).isIn(BudgetPlanStatus.DRAFT, BudgetPlanStatus.CLOSED);
            if (reloaded.status() == BudgetPlanStatus.DRAFT) {
                assertThat(outcomes).anyMatch(BudgetRuleViolationException.class::isInstance);
            }
        });
    }

    private BudgetPlan saveApproved(String planCode, String department) {
        BudgetPlan plan = planAdapter.save(BudgetPlan.create(
                planCode,
                "202601",
                department,
                "A100",
                new BigDecimal("100.00"),
                "maker"));
        plan.approve("checker");
        return planAdapter.save(plan);
    }

    private <T> List<T> runTogether(Callable<T> first, Callable<T> second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<T> one = executor.submit(awaitStart(ready, start, first));
        Future<T> two = executor.submit(awaitStart(ready, start, second));
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        return List.of(one.get(10, TimeUnit.SECONDS), two.get(10, TimeUnit.SECONDS));
    }

    private List<Object> runTogetherCapturing(Callable<?> first, Callable<?> second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<Object> one = executor.submit(awaitStart(ready, start, () -> capture(first)));
        Future<Object> two = executor.submit(awaitStart(ready, start, () -> capture(second)));
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        return List.of(one.get(10, TimeUnit.SECONDS), two.get(10, TimeUnit.SECONDS));
    }

    private static <T> Callable<T> awaitStart(
            CountDownLatch ready, CountDownLatch start, Callable<T> action) {
        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시 시작 gate timeout");
            }
            return action.call();
        };
    }

    private static Object capture(Callable<?> action) {
        try {
            return action.call();
        } catch (RuntimeException exception) {
            return exception;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
