package com.ho.account.loan.batch;

import com.ho.account.loan.LoanBatchApplication;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Exercises the real dev composition root against only Loan-owned migrations. */
@ActiveProfiles("dev")
@SpringBootTest(classes = LoanBatchApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.batch.job.enabled=false", "spring.batch.jdbc.initialize-schema=always",
                "spring.datasource.url=jdbc:h2:mem:loan_dev_isolation;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa", "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
                "spring.data.redis.repositories.enabled=false", "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false", "spring.cloud.loadbalancer.enabled=false",
                "spring.cloud.vault.enabled=false", "eureka.client.enabled=false",
                "management.tracing.enabled=false",
                "account.loan.remote.enabled=true",
                "account.loan.master-data-base-url=http://reference.invalid",
                "account.loan.journal-base-url=http://journal.invalid",
                "account.loan.accounting.cash-account-code=101000",
                "account.loan.accounting.loan-receivable-account-code=131000",
                "account.loan.accounting.deferred-asset-account-code=118000",
                "account.loan.accounting.recognized-income-account-code=410000",
                "account.loan.accounting.accrued-interest-receivable-account-code=115010",
                "account.loan.accounting.interest-income-account-code=410100"
        })
class LoanBatchDevProfileTest {
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired ApplicationContext context;
    @Autowired Environment environment;
    @Autowired DataSource dataSource;

    @DynamicPropertySource
    static void loanOnlyMigration(DynamicPropertyRegistry registry) throws IOException {
        // Match migration-runner/processResources: PostgreSQL V30 replaces generic V30;
        // all later Loan migrations are retained verbatim. Do not synthesize schema with JPA.
        Path resources = Path.of("../core/src/main/resources/db").toAbsolutePath().normalize();
        Path migrations = Files.createTempDirectory("loan-dev-postgresql-migrations-");
        migrations.toFile().deleteOnExit();
        try (var sources = Files.list(resources.resolve("migration"))) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".sql")).toList()) {
                Path effective = source.getFileName().toString().equals("V30__init_loan_schema.sql")
                        ? resources.resolve("postgresql-migration").resolve(source.getFileName()) : source;
                Path target = migrations.resolve(source.getFileName());
                Files.copy(effective, target);
                target.toFile().deleteOnExit();
            }
        }
        registry.add("spring.flyway.locations", () -> "filesystem:" + migrations);
    }

    @Test
    void devManagesOnlyLoanPersistenceAndUsesHttpPorts() throws Exception {
        assertThat(environment.matchesProfiles("dev")).isTrue();
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(entityManagerFactory.getMetamodel().getEntities())
                .isNotEmpty().allSatisfy(entity ->
                        assertThat(entity.getJavaType().getName()).startsWith("com.ho.account.loan."));
        assertBeanPresent("com.ho.account.loan.infrastructure.adapter.HttpLoanReferenceDataAdapter");
        assertBeanPresent("com.ho.account.loan.infrastructure.adapter.HttpLoanJournalAdapter");
        for (String type : new String[] {
                "com.ho.account.loan.infrastructure.adapter.LoanReferenceDataAdapter",
                "com.ho.account.loan.infrastructure.adapter.LoanJournalAdapter",
                "com.ho.account.masterdata.core.infrastructure.persistence.repository.BusinessPartnerRepository",
                "com.ho.account.shared.infrastructure.security.repository.AuditLogRepository",
                "com.ho.account.journalledger.domain.journal.repository.JournalEntryRepository",
                "com.ho.account.journalledger.application.port.in.JournalUseCase"
        }) {
            assertThat(context.getBeanNamesForType(Class.forName(type))).as(type).isEmpty();
        }
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement();
             var tables = statement.executeQuery("select table_name from information_schema.tables where table_schema = 'public'")) {
            var names = new java.util.ArrayList<String>();
            while (tables.next()) names.add(tables.getString(1));
            assertThat(names).contains("loans", "loan_disbursals", "flyway_schema_history")
                    .doesNotContain("journal_entries", "business_partners", "currencies", "audit_logs");
        }
    }

    private void assertBeanPresent(String type) throws ClassNotFoundException {
        assertThat(context.getBeanNamesForType(Class.forName(type))).as(type).hasSize(1);
    }
}
