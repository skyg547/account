package com.ho.account.budget.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.budget.application.port.out.BudgetExecutionPersistencePort;
import com.ho.account.budget.application.port.out.BudgetPlanPersistencePort;
import com.ho.account.budget.application.port.out.BudgetTransferPersistencePort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * API composition root가 실제 Flyway schema와 세 outbound adapter를 함께 조립하는 smoke test입니다.
 */
@SpringBootTest(
        classes = BudgetApiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "auth.jwt.secret=modern-account-system-test-secret-key-1234567890",
                "spring.datasource.url=jdbc:h2:mem:budget_api_context;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true"
        })
class BudgetApiApplicationContextTest {

    @Autowired
    private BudgetPlanPersistencePort budgetPlanPersistencePort;

    @Autowired
    private BudgetTransferPersistencePort budgetTransferPersistencePort;

    @Autowired
    private BudgetExecutionPersistencePort budgetExecutionPersistencePort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesBudgetSchemaAndCompositionRootUsesRealAdapters() {
        assertThat(budgetPlanPersistencePort).isNotNull();
        assertThat(budgetTransferPersistencePort).isNotNull();
        assertThat(budgetExecutionPersistencePort).isNotNull();

        Integer tableCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.tables
                where table_name in ('budget_plans', 'budget_transfers', 'budget_executions')
                """,
                Integer.class);
        assertThat(tableCount).isEqualTo(3);

        String version = jdbcTemplate.queryForObject(
                "select version from flyway_schema_history where success = true order by installed_rank desc limit 1",
                String.class);
        assertThat(version).isEqualTo("50");
    }
}
