package com.ho.account.asset.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AssetLeaseBatchLocalContextTest {

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private Environment environment;

    @Autowired
    private JobRegistry jobRegistry;

    @Autowired
    private JobExplorer jobExplorer;

    @Test
    void startsFromTheRealLocalH2ProfileWithoutLaunchingABusinessJob() throws Exception {

        assertThat(context).isInstanceOf(AnnotationConfigApplicationContext.class);
        assertThat(context.isActive()).isTrue();
        assertThat(environment.acceptsProfiles(Profiles.of("local"))).isTrue();
        assertThat(environment.getProperty("spring.main.web-application-type")).isEqualTo("none");
        assertThat(environment.getProperty("spring.datasource.url"))
                .startsWith("jdbc:h2:mem:asset_lease_batch")
                .contains("MODE=PostgreSQL");
        assertThat(environment.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("org.h2.Driver");
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                .isEqualTo("create-drop");
        assertThat(environment.getProperty("spring.sql.init.mode")).isEqualTo("never");
        assertThat(environment.getProperty("spring.batch.jdbc.initialize-schema"))
                .isEqualTo("always");
        assertThat(environment.getProperty("spring.batch.job.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.cloud.config.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.cloud.discovery.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.cloud.vault.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("eureka.client.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("eureka.client.register-with-eureka", Boolean.class))
                .isFalse();
        assertThat(environment.getProperty("eureka.client.fetch-registry", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.kafka.listener.auto-startup", Boolean.class))
                .isFalse();
        assertThat(context.getBeansOfType(JobLauncherApplicationRunner.class)).isEmpty();
        assertThat(jobRegistry.getJobNames()).containsExactly("assetDepreciationJob");
        assertThat(jobExplorer.getJobInstanceCount("assetDepreciationJob")).isZero();
    }
}
