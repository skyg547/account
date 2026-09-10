package com.ho.account.deposit;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.deposit.infrastructure.adapter.out.external.HttpDepositJournalPostingAdapter;
import com.ho.account.deposit.infrastructure.adapter.out.external.HttpDepositMasterDataAdapter;
import com.ho.account.deposit.infrastructure.adapter.out.local.LocalDepositJournalPostingAdapter;
import com.ho.account.deposit.infrastructure.adapter.out.local.LocalDepositMasterDataAdapter;
import com.ho.account.deposit.domain.DepositOutboxEntity;
import com.ho.account.contracts.outbox.OutboxStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("dev")
@SpringBootTest(classes = DepositApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:deposit_dev_runtime;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa", "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
                "spring.cloud.config.enabled=false", "spring.cloud.discovery.enabled=false",
                "spring.cloud.loadbalancer.enabled=false", "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false", "management.tracing.enabled=false",
                "account.deposit.remote.enabled=true", "account.deposit.local-adapters.enabled=false",
                "account.deposit.master-data-base-url=http://reference.invalid",
                "account.deposit.journal-base-url=http://journal.invalid",
                "account.deposit.account-mapping.cash-account-code=10100",
                "account.deposit.account-mapping.deposit-liability-account-code=20200",
                "account.deposit.outbox.scheduler.enabled=false"
        })
class DepositDevRuntimeContextTest {
    @Autowired ApplicationContext context;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired DataSource dataSource;
    @PersistenceContext EntityManager entityManager;

    @DynamicPropertySource
    static void publishedDepositMigrations(DynamicPropertyRegistry registry) throws IOException {
        // Match migration-runner: PostgreSQL overrides only V40 and V42; retain generic V41.
        Path resources = Path.of("../core/src/main/resources/db").toAbsolutePath().normalize();
        Path migrations = Files.createTempDirectory("deposit-dev-postgresql-migrations-");
        migrations.toFile().deleteOnExit();
        try (var sources = Files.list(resources.resolve("migration"))) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".sql")).toList()) {
                String name = source.getFileName().toString();
                Path effective = name.startsWith("V40__") || name.startsWith("V42__")
                        ? resources.resolve("postgresql-migration").resolve(name) : source;
                Path target = migrations.resolve(name);
                Files.copy(effective, target);
                target.toFile().deleteOnExit();
            }
        }
        registry.add("spring.flyway.locations", () -> "filesystem:" + migrations);
    }

    @Test
    @Transactional
    void textPayloadRoundTripsLongUnicodeThroughMigratedSchema() {
        String payload = "{\"description\":\"" + "예금 입금 내역 € 漢字 🏦 ".repeat(1000) + "\"}";
        assertThat(payload.length()).isGreaterThan(10_000);
        var entity = new DepositOutboxEntity();
        entity.setEventId("deposit-dev-unicode-event");
        entity.setSourceModule("DEPOSIT");
        entity.setLineageSourceType("DEPOSIT_ACCOUNT");
        entity.setLineageSourceId("DEP-UNICODE");
        entity.setIdempotencyKey("DEPOSIT_ACCOUNT:DEP-UNICODE");
        entity.setPayload(payload);
        entityManager.persist(entity);
        entityManager.flush();
        Long id = entity.getId();
        entityManager.clear();

        DepositOutboxEntity reloaded = entityManager.find(DepositOutboxEntity.class, id);
        assertThat(reloaded).isNotNull();
        assertThat(reloaded).isNotSameAs(entity);
        assertThat(reloaded.getPayload()).isEqualTo(payload);
        assertThat(reloaded.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(reloaded.getEventType()).isEqualTo("JOURNAL_ENTRY");
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getRetryCount()).isZero();
    }

    @Test
    void devValidatesPublishedSchemaAndUsesRealHttpPortsWithOnlyDepositEntities() throws Exception {
        assertThat(context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(context.getBeansOfType(MasterDataQueryPort.class)).hasSize(1);
        assertThat(context.getBean(MasterDataQueryPort.class)).isInstanceOf(HttpDepositMasterDataAdapter.class);
        assertThat(context.getBeansOfType(JournalPostingPort.class)).hasSize(1);
        assertThat(context.getBean(JournalPostingPort.class)).isInstanceOf(HttpDepositJournalPostingAdapter.class);
        assertThat(context.getBeansOfType(LocalDepositMasterDataAdapter.class)).isEmpty();
        assertThat(context.getBeansOfType(LocalDepositJournalPostingAdapter.class)).isEmpty();
        assertThat(entityManagerFactory.getMetamodel().getEntities()).isNotEmpty().allSatisfy(entity ->
                assertThat(entity.getJavaType().getName()).startsWith("com.ho.account.deposit."));
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement();
             var tables = statement.executeQuery("select table_name from information_schema.tables where table_schema = 'public'")) {
            var names = new java.util.ArrayList<String>();
            while (tables.next()) names.add(tables.getString(1));
            assertThat(names).contains("deposit_accounts", "deposit_transactions", "deposit_outbox", "flyway_schema_history")
                    .doesNotContain("journal_entries", "business_partners", "account_subjects", "audit_logs");
        }
    }
}
