package com.ho.account.closing.batch;

import com.ho.account.closing.application.port.out.FxExchangeRateLookupPort;
import com.ho.account.closing.batch.config.ClosingBatchMonolithConfiguration;
import com.ho.account.closing.infrastructure.external.HttpClosingJournalAdapter;
import com.ho.account.closing.infrastructure.source.ClosingReadOnlySources;
import com.ho.account.closing.infrastructure.source.JdbcClosingMasterDataAdapter;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Actual dev composition: only Closing migrations and metadata touch the primary datasource. */
@ActiveProfiles("dev")
@SpringBootTest(classes = ClosingBatchApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.batch.job.enabled=false", "spring.batch.jdbc.initialize-schema=always",
                "spring.cloud.config.enabled=false", "spring.cloud.vault.enabled=false",
                "spring.cloud.discovery.enabled=false", "spring.cloud.loadbalancer.enabled=false",
                "eureka.client.enabled=false", "management.tracing.enabled=false",
                "spring.data.redis.repositories.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:closing-batch-dev;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa", "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/closing-migration",
                "spring.flyway.table=flyway_schema_history_closing",
                "spring.flyway.baseline-on-migrate=false", "spring.sql.init.mode=never",
                "closing.journal-ledger.base-url=http://journal.invalid",
                "closing.sources.enabled=true",
                "closing.sources.journal.url=jdbc:postgresql://journal.invalid/seed",
                "closing.sources.journal.username=seed_test", "closing.sources.journal.password=synthetic",
                "closing.sources.ecl.url=jdbc:postgresql://ecl.invalid/seed",
                "closing.sources.ecl.username=seed_test", "closing.sources.ecl.password=synthetic",
                "closing.sources.master-data.url=jdbc:postgresql://master.invalid/seed",
                "closing.sources.master-data.username=seed_test", "closing.sources.master-data.password=synthetic"
        })
class ClosingBatchDevProfileTest {
    @Autowired ApplicationContext context;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired JdbcTemplate jdbc;

    @Test
    void devOwnsOnlyClosingEntitiesAndPrimaryDataSource() {
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM flyway_schema_history_closing
                WHERE version IN ('49', '50', '51', '52') AND success = TRUE
                """, Integer.class)).isEqualTo(4);
        assertThat(entityManagerFactory.getMetamodel().getEntities()).isNotEmpty()
                .allSatisfy(entity -> assertThat(entity.getJavaType().getPackageName())
                        .isEqualTo("com.ho.account.closing.domain"));
        assertThat(context.getBeansOfType(DataSource.class)).hasSize(1);
        assertThat(context.getBeansOfType(ClosingReadOnlySources.class)).hasSize(1);
        assertThat(context.getBeansOfType(ClosingBatchMonolithConfiguration.class)).isEmpty();
    }

    @Test
    void remoteJournalAndReadOnlyMasterAdapterReplaceEmbeddedProviders() {
        assertThat(context.getBeansOfType(JournalPostingPort.class).values())
                .singleElement().isInstanceOf(HttpClosingJournalAdapter.class);
        assertThat(context.getBeansOfType(FxExchangeRateLookupPort.class).values())
                .singleElement().isInstanceOf(JdbcClosingMasterDataAdapter.class);
        assertThat(context.getBean(FxExchangeRateLookupPort.class))
                .isSameAs(context.getBean(MasterDataQueryPort.class));
    }
}
