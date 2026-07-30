package com.ho.account.budget.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.budget.application.exception.BudgetConflictException;
import com.ho.account.budget.domain.BudgetExecution;
import com.ho.account.budget.domain.BudgetExecutionStatus;
import com.ho.account.budget.domain.BudgetFiscalYearControl;
import com.ho.account.budget.domain.BudgetFiscalYearStatus;
import com.ho.account.budget.domain.BudgetPlan;
import com.ho.account.budget.domain.BudgetPlanStatus;
import com.ho.account.budget.domain.BudgetTransfer;
import com.ho.account.budget.domain.BudgetTransferStatus;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none"
})
@ContextConfiguration(classes = BudgetPersistenceAdapterTest.TestApplication.class)
@Sql(scripts = "/db/migration/V50__create_budget_control_tables.sql",
        executionPhase = ExecutionPhase.BEFORE_TEST_CLASS)
@Import({
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
class BudgetPersistenceAdapterTest {

    @Autowired
    private JpaBudgetPlanPersistenceAdapter planAdapter;

    @Autowired
    private JpaBudgetTransferPersistenceAdapter transferAdapter;

    @Autowired
    private JpaBudgetExecutionPersistenceAdapter executionAdapter;

    @Autowired
    private JpaBudgetFiscalYearControlPersistenceAdapter fiscalAdapter;

    @Autowired
    private JpaBudgetIdempotencyLockAdapter idempotencyAdapter;

    @Autowired
    private SpringDataBudgetFiscalYearControlRepository fiscalRepository;

    @Autowired
    private SpringDataBudgetIdempotencyShardRepository shardRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = BudgetPlanJpaEntity.class)
    @EnableJpaRepositories(basePackageClasses = SpringDataBudgetPlanRepository.class)
    static class TestApplication {
    }

    @Test
    void databaseEnforcesPlanBusinessKeyUniqueness() {
        planAdapter.save(draftPlan("PLAN-1", "202601", "D001", "A100"));

        assertThatThrownBy(() -> planAdapter.save(draftPlan("PLAN-2", "202601", "D001", "A100")))
                .isInstanceOf(BudgetConflictException.class);
    }

    @Test
    void migrationSeedsEveryFiscalYearAndAllIdempotencyShardsAndAdaptersLockThem() {
        assertThat(fiscalRepository.count()).isEqualTo(10_000);
        assertThat(shardRepository.count()).isEqualTo(256);
        assertThat(jdbcTemplate.queryForObject(
                        "select min(fiscal_year) from budget_fiscal_year_controls", String.class))
                .isEqualTo("0000");
        assertThat(jdbcTemplate.queryForObject(
                        "select max(fiscal_year) from budget_fiscal_year_controls", String.class))
                .isEqualTo("9999");

        assertThat(fiscalAdapter.findByFiscalYearForUpdate("2026"))
                .get()
                .extracting(BudgetFiscalYearControl::status)
                .isEqualTo(BudgetFiscalYearStatus.OPEN);
        idempotencyAdapter.lockTransferRequestKey("REQ-LOCK");
        idempotencyAdapter.lockExecutionSourceKey("AP", "INV-LOCK", "LINE-1");
    }

    @Test
    void fiscalCloseRoundTripsAuditAndIncrementsVersionThroughLockedAdapter() {
        BudgetFiscalYearControlJpaEntity before = fiscalRepository.findById("2026").orElseThrow();
        long initialVersion = before.getVersion();
        entityManager.clear();

        BudgetFiscalYearControl control =
                fiscalAdapter.findByFiscalYearForUpdate("2026").orElseThrow();
        control.close("closer");
        BudgetFiscalYearControl saved = fiscalAdapter.save(control);
        entityManager.clear();

        BudgetFiscalYearControl reloaded =
                fiscalAdapter.findByFiscalYearForUpdate("2026").orElseThrow();
        BudgetFiscalYearControlJpaEntity row = fiscalRepository.findById("2026").orElseThrow();
        assertThat(saved.status()).isEqualTo(BudgetFiscalYearStatus.CLOSED);
        assertThat(reloaded.status()).isEqualTo(BudgetFiscalYearStatus.CLOSED);
        assertThat(reloaded.closedBy()).isEqualTo("closer");
        assertThat(row.getClosedAt()).isNotNull();
        assertThat(row.getVersion()).isGreaterThan(initialVersion);
    }

    @Test
    void databaseEnforcesTransferRequestKeyUniqueness() {
        BudgetPlan source = planAdapter.save(draftPlan("SOURCE", "202601", "D001", "A100"));
        BudgetPlan target = planAdapter.save(draftPlan("TARGET", "202601", "D002", "A100"));
        transferAdapter.save(BudgetTransfer.request(
                "REQ-1", source.id(), target.id(), new BigDecimal("10.00"), "maker"));

        assertThatThrownBy(() -> transferAdapter.save(BudgetTransfer.request(
                        "REQ-1", target.id(), source.id(), new BigDecimal("5.00"), "other-maker")))
                .isInstanceOf(BudgetConflictException.class);
    }

    @Test
    void databaseEnforcesExecutionSourceTripleUniqueness() {
        BudgetPlan first = planAdapter.save(draftPlan("PLAN-1", "202601", "D001", "A100"));
        BudgetPlan second = planAdapter.save(draftPlan("PLAN-2", "202601", "D002", "A100"));
        LocalDate date = LocalDate.of(2026, 1, 31);
        executionAdapter.save(BudgetExecution.execute(
                first.id(), "AP", "INV-1", "LINE-1", date, new BigDecimal("10.00"), "executor"));

        assertThatThrownBy(() -> executionAdapter.save(BudgetExecution.execute(
                        second.id(),
                        "AP",
                        "INV-1",
                        "LINE-1",
                        date,
                        new BigDecimal("5.00"),
                        "other-executor")))
                .isInstanceOf(BudgetConflictException.class);
    }

    @Test
    void lockedQueriesRoundTripPlanTransferExecutionAndPreserveOrderAndLifecycle() {
        BudgetPlan higher = approved(planAdapter.save(draftPlan("HIGHER", "202612", "D002", "A100")));
        higher = planAdapter.save(higher);
        BudgetPlan lower = approved(planAdapter.save(draftPlan("LOWER", "202601", "D001", "A100")));
        lower = planAdapter.save(lower);

        BudgetTransfer transfer = transferAdapter.save(BudgetTransfer.request(
                "REQ-LOCK", higher.id(), lower.id(), new BigDecimal("12.50"), "maker"));
        BudgetExecution execution = executionAdapter.save(BudgetExecution.execute(
                lower.id(),
                "AP",
                "INV-LOCK",
                "LINE-1",
                LocalDate.of(2026, 1, 31),
                new BigDecimal("8.25"),
                "executor"));
        entityManager.clear();

        BudgetPlan lockedById = planAdapter.findByIdForUpdate(lower.id()).orElseThrow();
        BudgetPlan lockedByBusinessKey =
                planAdapter.findByBusinessKeyForUpdate("202601", "D001", "A100").orElseThrow();
        assertThat(lockedById.id()).isEqualTo(lower.id());
        assertThat(lockedByBusinessKey.status()).isEqualTo(BudgetPlanStatus.APPROVED);
        assertThat(lockedByBusinessKey.approvedBy()).isEqualTo("checker");
        assertThat(planAdapter.findApprovedByFiscalYearForUpdateOrderById("2026"))
                .extracting(BudgetPlan::id)
                .containsExactly(higher.id(), lower.id());

        BudgetTransfer lockedTransfer =
                transferAdapter.findByRequestKeyForUpdate("REQ-LOCK").orElseThrow();
        assertThat(lockedTransfer.sourcePlanId()).isEqualTo(higher.id());
        assertThat(lockedTransfer.targetPlanId()).isEqualTo(lower.id());
        lockedTransfer.approve("approver");
        BudgetTransfer approvedTransfer = transferAdapter.save(lockedTransfer);
        assertThat(approvedTransfer.status()).isEqualTo(BudgetTransferStatus.APPROVED);
        assertThat(approvedTransfer.approvedBy()).isEqualTo("approver");

        BudgetExecution lockedExecution =
                executionAdapter.findBySourceForUpdate("AP", "INV-LOCK", "LINE-1").orElseThrow();
        assertThat(lockedExecution.executionDate()).isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(lockedExecution.amount()).isEqualByComparingTo("8.25");
        lockedExecution.cancel("canceller");
        BudgetExecution cancelled = executionAdapter.save(lockedExecution);
        assertThat(cancelled.status()).isEqualTo(BudgetExecutionStatus.CANCELLED);
        assertThat(cancelled.cancelledBy()).isEqualTo("canceller");
    }

    private static BudgetPlan draftPlan(
            String planCode, String yearMonth, String departmentCode, String accountCode) {
        return BudgetPlan.create(
                planCode,
                yearMonth,
                departmentCode,
                accountCode,
                new BigDecimal("100.00"),
                "maker");
    }

    private static BudgetPlan approved(BudgetPlan plan) {
        plan.approve("checker");
        return plan;
    }
}
