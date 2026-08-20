package com.ho.account.closing;

import com.ho.account.masterdata.core.infrastructure.adapter.MonolithFiscalPeriodControlAdapter;
import com.ho.account.masterdata.core.infrastructure.adapter.MonolithMasterDataQueryAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.JpaFiscalPeriodPersistenceAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.FiscalPeriodMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("local")
@SpringBootTest(
        classes = ClosingApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.loadbalancer.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false",
                "management.tracing.enabled=false",
                "spring.data.redis.repositories.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:closing-api-context;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false"
        })
class ClosingApplicationContextTest {

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoadsWithOnlyClosingRuntimeBoundaries() {
        assertThat(environment.getProperty("spring.application.name"))
                .isEqualTo("closing-service");
        assertThat(environment.getProperty("spring.flyway.locations"))
                .isEqualTo("classpath:db/closing-migration");
        assertThat(environment.getProperty("spring.flyway.table"))
                .isEqualTo("flyway_schema_history_closing");
        assertThat(environment.getProperty("spring.flyway.baseline-version"))
                .isEqualTo("49");
        assertThat(applicationContext.getBeansOfType(FiscalPeriodMapper.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(JpaFiscalPeriodPersistenceAdapter.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(MonolithFiscalPeriodControlAdapter.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(MonolithMasterDataQueryAdapter.class)).hasSize(1);
    }
}
