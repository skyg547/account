package com.ho.account.tax.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.tax.repository.TaxInvoiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
        classes = TaxApiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.profiles.active=local",
                "spring.datasource.url=jdbc:h2:mem:tax-api-pg-schema;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/tax-migration",
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
class TaxPostgresqlSchemaContextTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TaxInvoiceRepository repository;

    @Test
    void cleanBaselineMigratesValidatesAndSupportsRepository() {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM flyway_schema_history
                WHERE version = '1' AND success = TRUE
                """, Integer.class)).isOne();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name = 'tax_invoices'
                """, Integer.class)).isOne();
        assertThat(repository.count()).isZero();
    }
}

