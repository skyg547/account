package com.ho.account.budget.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.budget.application.port.out.BudgetFiscalYearControlPersistencePort;
import com.ho.account.budget.application.port.out.BudgetIdempotencyLockPort;
import com.ho.account.budget.application.port.out.BudgetExecutionPersistencePort;
import com.ho.account.budget.application.port.out.BudgetPlanPersistencePort;
import com.ho.account.budget.application.port.out.BudgetTransferPersistencePort;
import com.ho.account.budget.domain.BudgetFiscalYearStatus;
import com.ho.account.budget.infrastructure.persistence.JpaBudgetFiscalYearControlPersistenceAdapter;
import com.ho.account.budget.infrastructure.persistence.JpaBudgetIdempotencyLockAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영 composition root가 웹 서버 없이 core와 Batch Job을 조립하고 JobRegistry에 등록하는지 확인한다.
 *
 * <p>실제 Flyway V50과 JPA adapter까지 함께 올립니다. Batch 단위 테스트가 use case를 mock으로
 * 격리하더라도 composition root에서는 운영과 같은 bean 조립이 반드시 한 번 검증되어야 합니다.
 */
@SpringBootTest(
        classes = BudgetBatchApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.batch.job.enabled=false",
                "spring.batch.jdbc.initialize-schema=always",
                "spring.datasource.url=jdbc:h2:mem:budget_batch_context;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true"
        })
class BudgetBatchApplicationContextTest {

    @Autowired
    private BudgetPlanPersistencePort budgetPlanPersistencePort;

    @Autowired
    private BudgetTransferPersistencePort budgetTransferPersistencePort;

    @Autowired
    private BudgetExecutionPersistencePort budgetExecutionPersistencePort;

    @Autowired
    private BudgetFiscalYearControlPersistencePort budgetFiscalYearControlPersistencePort;

    @Autowired
    private BudgetIdempotencyLockPort budgetIdempotencyLockPort;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    @Autowired
    private JobRegistry jobRegistry;

    @Autowired
    private Job budgetYearEndCloseJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void compositionRootIsNonWebAndRegistersTheYearEndJobByStableName() throws Exception {
        assertThat(applicationContext.getBean(BudgetBatchApplication.class)).isNotNull();
        assertThat(environment.getProperty("spring.main.web-application-type")).isEqualTo("none");
        assertThat(environment.getProperty("spring.batch.job.enabled", Boolean.class)).isFalse();
        assertThat(jobRegistry.getJob(BudgetYearEndCloseJobConfiguration.JOB_NAME))
                .isSameAs(budgetYearEndCloseJob);
        assertThat(budgetPlanPersistencePort).isNotNull();
        assertThat(budgetTransferPersistencePort).isNotNull();
        assertThat(budgetExecutionPersistencePort).isNotNull();
    }

    @Test
    void flywayV50CreatesAndSeedsDurableFiscalYearAndIdempotencyLocks() {
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM budget_fiscal_year_controls", Long.class))
                .isEqualTo(10_000L);
        assertThat(jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM budget_fiscal_year_controls
                        WHERE fiscal_year = '2026' AND status = 'OPEN' AND version = 0
                        """,
                        Long.class))
                .isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM budget_idempotency_shards", Long.class))
                .isEqualTo(256L);
    }

    @Test
    @Transactional
    void durableControlAndLockPortsComposeWithTheirRealJpaAdapters() {
        assertThat(budgetFiscalYearControlPersistencePort)
                .isInstanceOf(JpaBudgetFiscalYearControlPersistenceAdapter.class);
        assertThat(budgetIdempotencyLockPort)
                .isInstanceOf(JpaBudgetIdempotencyLockAdapter.class);

        assertThat(budgetFiscalYearControlPersistencePort.findByFiscalYearForUpdate("2026"))
                .get()
                .satisfies(control -> {
                    assertThat(control.fiscalYear()).isEqualTo("2026");
                    assertThat(control.status()).isEqualTo(BudgetFiscalYearStatus.OPEN);
                });

        // 두 호출이 완료되면 실제 JPA adapter가 사전 생성 shard 행을 비관적으로 잠근 것이다.
        budgetIdempotencyLockPort.lockTransferRequestKey("batch-context-transfer");
        budgetIdempotencyLockPort.lockExecutionSourceKey(
                "BATCH_CONTEXT", "source-1", "line-1");
    }
}
