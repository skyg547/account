package com.ho.account.closing;

import com.ho.account.closing.application.port.out.RetainedEarningsMappingPort;
import com.ho.account.closing.config.ClosingMonolithConfiguration;
import com.ho.account.closing.infrastructure.config.AnnualClosingConfigurationProperties;
import com.ho.account.closing.infrastructure.config.ConfiguredRetainedEarningsMappingAdapter;
import com.ho.account.closing.infrastructure.external.HttpClosingJournalAdapter;
import com.ho.account.closing.infrastructure.external.HttpClosingMasterDataQueryAdapter;
import com.ho.account.closing.infrastructure.external.HttpFiscalPeriodControlAdapter;
import com.ho.account.closing.infrastructure.local.ClosingLocalExternalPortConfiguration;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.support.Repositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("dev")
@SpringBootTest(classes = ClosingApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.vault.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.loadbalancer.enabled=false",
                "eureka.client.enabled=false",
                "management.tracing.enabled=false",
                "spring.data.redis.repositories.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:closing-dev-runtime;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/closing-migration",
                "spring.flyway.table=flyway_schema_history_closing",
                "spring.flyway.baseline-on-migrate=false",
                "spring.sql.init.mode=never",
                "closing.master-data.remote.enabled=true",
                "closing.master-data.base-url=http://master-data.test",
                "closing.journal-ledger.base-url=http://journal-ledger.test",
                "closing.sources.enabled=true",
                "closing.sources.journal.url=jdbc:postgresql://localhost:1/closing-dev-journal",
                "closing.sources.journal.username=test",
                "closing.sources.journal.password=test",
                "closing.sources.ecl.url=jdbc:postgresql://localhost:1/closing-dev-ecl",
                "closing.sources.ecl.username=test",
                "closing.sources.ecl.password=test",
                "closing.sources.master-data.url=jdbc:postgresql://localhost:1/closing-dev-master",
                "closing.sources.master-data.username=test",
                "closing.sources.master-data.password=test",
                "account.closing.annual.legal-entity-code=ENTITY-TEST",
                "account.closing.annual.mappings[0].fiscal-year=2026",
                "account.closing.annual.mappings[0].account-code=35000",
                "account.closing.annual.mappings[0].postable=true",
                "account.closing.annual.mappings[0].approved-by=test-controller",
                "account.closing.annual.mappings[0].change-reference=TEST-776"
        })
class ClosingDevRuntimeContextTest {
    @Autowired ApplicationContext context;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired JdbcTemplate jdbc;

    @Test
    void actualDevApplicationValidatesOnlyClosingSchemaAfterOwnedMigrations() {
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM flyway_schema_history_closing
                WHERE version IN ('49', '50', '51', '52', '53') AND success = TRUE
                """, Integer.class)).isEqualTo(5);
        assertThat(entityManagerFactory.getMetamodel().getEntities()).isNotEmpty()
                .allSatisfy(entity -> assertThat(entity.getJavaType().getPackageName())
                        .isIn("com.ho.account.closing.domain", "com.ho.account.closing.infrastructure.persistence"));
        assertThat(context.getBeansOfType(Repository.class)).isNotEmpty();
        new Repositories(context).forEach(domain -> assertThat(domain.getPackageName())
                .isIn("com.ho.account.closing.domain", "com.ho.account.closing.infrastructure.persistence"));
    }

    @Test
    void devUsesExactlyOneRemoteProviderPerPortWithoutMonolithOrLocalFakes() {
        assertThat(context.getBeansOfType(FiscalPeriodControlPort.class).values())
                .singleElement().isInstanceOf(HttpFiscalPeriodControlAdapter.class);
        assertThat(context.getBeansOfType(JournalQueryPort.class).values())
                .singleElement().isInstanceOf(HttpClosingJournalAdapter.class);
        assertThat(context.getBeansOfType(JournalPostingPort.class).values())
                .singleElement().isSameAs(context.getBean(JournalQueryPort.class));
        assertThat(context.getBeansOfType(MasterDataQueryPort.class).values())
                .singleElement().isInstanceOf(HttpClosingMasterDataQueryAdapter.class);
        assertThat(context.getBeansOfType(AnnualClosingConfigurationProperties.class).values())
                .singleElement();
        assertThat(context.getBeansOfType(RetainedEarningsMappingPort.class).values())
                .singleElement().isInstanceOf(ConfiguredRetainedEarningsMappingAdapter.class);
        assertThat(context.getBeansOfType(ClosingMonolithConfiguration.class)).isEmpty();
        assertThat(context.getBeansOfType(ClosingLocalExternalPortConfiguration.class)).isEmpty();
    }
}
