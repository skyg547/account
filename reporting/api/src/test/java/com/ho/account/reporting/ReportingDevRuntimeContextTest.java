package com.ho.account.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import com.ho.account.contracts.ledger.LedgerQueryPort;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.reporting.application.port.out.LoadLedgerPort;
import com.ho.account.reporting.infrastructure.persistence.LedgerClientAdapter;
import com.ho.account.reporting.infrastructure.persistence.InMemoryLedgerBalanceAdapter;
import com.ho.account.reporting.infrastructure.persistence.InMemoryJournalQueryAdapter;
import com.ho.account.reporting.infrastructure.external.HttpReportingLedgerAdapter;
import com.ho.account.reporting.infrastructure.external.HttpReportingJournalAdapter;
import jakarta.persistence.EntityManagerFactory;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Real dev composition and release migrations; no mocked application ports or generated DDL. */
@ActiveProfiles("dev")
@SpringBootTest(classes = ReportingApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:reporting_dev_runtime;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
                "spring.datasource.password=", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
                "spring.cloud.config.enabled=false", "spring.cloud.discovery.enabled=false",
                "spring.cloud.loadbalancer.enabled=false", "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false", "spring.data.redis.repositories.enabled=false",
                "management.tracing.enabled=false", "account.reporting.persistence.mode=jpa",
                "account.reporting.remote.enabled=true", "account.reporting.journal-base-url=http://journal.invalid"
        })
class ReportingDevRuntimeContextTest {
    @Autowired ApplicationContext context;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired Environment environment;
    @Autowired DataSource dataSource;

    @DynamicPropertySource
    static void reportingOnlyMigrations(DynamicPropertyRegistry registry) {
        // migration-runner packages these V60–63 unchanged, without PostgreSQL overrides.
        registry.add("spring.flyway.locations", () -> "filesystem:" +
                Path.of("../core/src/main/resources/db/migration").toAbsolutePath().normalize());
    }

    @Test
    void realDevContextUsesJpaAndHttpWithOnlyReportingSchema() throws Exception {
        assertThat(environment.matchesProfiles("dev")).isTrue();
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(context.getBean(LoadLedgerPort.class)).isInstanceOf(LedgerClientAdapter.class);
        assertThat(context.getBean(LedgerQueryPort.class)).isInstanceOf(HttpReportingLedgerAdapter.class);
        assertThat(context.getBean(JournalQueryPort.class)).isInstanceOf(HttpReportingJournalAdapter.class);
        assertThat(context.getBeanNamesForType(InMemoryLedgerBalanceAdapter.class)).isEmpty();
        assertThat(context.getBeanNamesForType(InMemoryJournalQueryAdapter.class)).isEmpty();
        assertThat(entityManagerFactory.getMetamodel().getEntities()).isNotEmpty()
                .allSatisfy(entity -> assertThat(entity.getJavaType().getName()).startsWith("com.ho.account.reporting."));
        for (String type : new String[] {
                "com.ho.account.masterdata.core.infrastructure.persistence.repository.BusinessPartnerRepository",
                "com.ho.account.shared.infrastructure.security.repository.AuditLogRepository"
        }) assertThat(context.getBeanNamesForType(Class.forName(type))).as(type).isEmpty();
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement();
             var result = statement.executeQuery("select table_name from information_schema.tables where table_schema='public'")) {
            var tables = new java.util.ArrayList<String>();
            while (result.next()) tables.add(result.getString(1));
            assertThat(tables).contains("rpt_line_mapping", "rpt_snapshot_header", "rpt_snapshot_detail",
                    "rpt_regulatory_submission", "rpt_disclosure_note_mart", "rpt_regulatory_filing", "flyway_schema_history")
                    .doesNotContain("gl_balance", "sl_balance", "journal_entries", "business_partners", "audit_logs");
        }
    }
}
