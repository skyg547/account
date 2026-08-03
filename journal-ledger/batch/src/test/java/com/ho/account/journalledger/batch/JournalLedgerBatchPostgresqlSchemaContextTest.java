package com.ho.account.journalledger.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
        classes = JournalLedgerBatchPostgresqlSchemaContextTest.SchemaValidationApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.config.name=schema-validation",
                "spring.profiles.active=local",
                "spring.datasource.url=jdbc:h2:mem:journal-batch-pg-schema;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/journal-migration",
                "spring.flyway.clean-disabled=true",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.batch.job.enabled=false",
                "spring.batch.jdbc.initialize-schema=always",
                "spring.sql.init.mode=never",
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false"
        })
class JournalLedgerBatchPostgresqlSchemaContextTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void batchMetadataCoexistsWithJournalSchemaValidation() {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM flyway_schema_history
                WHERE version = '11' AND success = TRUE
                """, Integer.class)).isOne();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name = 'batch_job_instance'
                """, Integer.class)).isOne();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.ho.account.journalledger.domain")
    @EnableJpaRepositories(basePackages = {
            "com.ho.account.journalledger.domain",
            "com.ho.account.journalledger.adapter.out.persistence"
    })
    static class SchemaValidationApplication {
    }
}
