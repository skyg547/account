package com.ho.account.closing.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
        classes = ClosingBatchPostgresqlSchemaContextTest.SchemaValidationApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.profiles.active=prod",
                "spring.datasource.url=jdbc:h2:mem:closing-batch-pg-schema;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/closing-migration",
                "spring.flyway.table=flyway_schema_history_closing",
                "spring.flyway.baseline-on-migrate=false",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.batch.job.enabled=false",
                "spring.batch.jdbc.initialize-schema=never",
                "spring.sql.init.mode=never",
                "spring.cloud.discovery.enabled=false",
                "eureka.client.enabled=false"
        })
class ClosingBatchPostgresqlSchemaContextTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void startsWithoutAJobAndValidatesJpaAfterCleanClosingMigrations(
            ConfigurableApplicationContext context) {
        assertThat(context.isActive()).isTrue();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM flyway_schema_history_closing
                WHERE version IN ('49', '50', '51') AND success = TRUE
                """, Integer.class)).isEqualTo(3);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.ho.account.closing.domain")
    @EnableJpaRepositories(basePackages = "com.ho.account.closing.infrastructure.persistence")
    static class SchemaValidationApplication {
    }
}
