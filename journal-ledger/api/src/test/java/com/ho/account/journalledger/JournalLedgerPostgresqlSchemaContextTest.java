package com.ho.account.journalledger;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.journalledger.domain.journal.repository.JournalEntryRepository;
import com.ho.account.journalledger.domain.ledger.repository.GlBalanceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
        classes = JournalLedgerPostgresqlSchemaContextTest.SchemaValidationApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.config.name=schema-validation",
                "spring.profiles.active=local",
                "spring.datasource.url=jdbc:h2:mem:journal-api-pg-schema;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/journal-migration",
                "spring.flyway.clean-disabled=true",
                "spring.flyway.baseline-on-migrate=false",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.sql.init.mode=never",
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false"
        })
class JournalLedgerPostgresqlSchemaContextTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private GlBalanceRepository glBalanceRepository;

    @Test
    void cleanBaselineMigratesValidatesAndSupportsCoreRepositories() {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE version IN ('1', '10', '11') AND success = TRUE
                """, Integer.class)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN (
                    'journal_entries', 'journal_details', 'journal_rules',
                    'journal_rule_conditions', 'journal_rule_details',
                    'gl_entries', 'sl_entries', 'gl_balances', 'sl_balances', 'unsettled_items')
                """, Integer.class)).isEqualTo(10);
        assertThat(journalEntryRepository.count()).isZero();
        assertThat(glBalanceRepository.count()).isZero();
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
