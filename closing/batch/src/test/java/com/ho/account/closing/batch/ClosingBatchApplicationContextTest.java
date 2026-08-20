package com.ho.account.closing.batch;

import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.masterdata.core.infrastructure.adapter.MonolithExchangeRateQueryAdapter;
import com.ho.account.masterdata.core.infrastructure.adapter.MonolithFiscalPeriodControlAdapter;
import com.ho.account.masterdata.core.infrastructure.adapter.MonolithMasterDataQueryAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.JpaFiscalPeriodPersistenceAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.FiscalPeriodMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("local")
@SpringBootTest(
        classes = ClosingBatchApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.batch.job.enabled=false",
                "spring.batch.jdbc.initialize-schema=always",
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.loadbalancer.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false",
                "management.tracing.enabled=false",
                "spring.data.redis.repositories.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:closing-batch-context;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false"
        })
class ClosingBatchApplicationContextTest {

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoadsWithoutStartingAJob() {
        assertThat(environment.getProperty("spring.application.name"))
                .isEqualTo("closing-batch");
        assertThat(applicationContext.getBeansOfType(JournalPostingPort.class))
                .containsOnlyKeys("closingLocalJournalPostingPort");
        assertThat(applicationContext.getBeansOfType(JournalQueryPort.class))
                .containsOnlyKeys("closingLocalJournalQueryPort");
        assertThat(applicationContext.getBeansOfType(MonolithExchangeRateQueryAdapter.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(FiscalPeriodMapper.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(JpaFiscalPeriodPersistenceAdapter.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(MonolithFiscalPeriodControlAdapter.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(MonolithMasterDataQueryAdapter.class)).hasSize(1);
    }
}
