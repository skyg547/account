package com.ho.account.reconciliation.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.reconciliation.repository.ReconciliationUnitRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
        classes = ReconciliationApiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.profiles.active=local",
                "spring.datasource.url=jdbc:h2:mem:reconciliation-api-pg-schema;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/reconciliation-migration",
                "spring.flyway.clean-disabled=true",
                "spring.flyway.baseline-on-migrate=false",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.sql.init.mode=never",
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false",
                "management.tracing.enabled=false"
        })
class ReconciliationPostgresqlSchemaContextTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReconciliationUnitRepository repository;

    @Test
    void cleanBaselineMigratesValidatesAndSupportsRepository() {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM flyway_schema_history
                WHERE version = '1' AND success = TRUE
                """, Integer.class)).isOne();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN (
                    'reconciliation_units', 'difference_reason_codes', 'bank_statements',
                    'recon_external_stage_record', 'reconciliation_rules', 'reconciliation_runs',
                    'reconciliation_differences', 'reconciliation_stage_results')
                """, Integer.class)).isEqualTo(8);
        assertThat(repository.count()).isZero();
    }
}

