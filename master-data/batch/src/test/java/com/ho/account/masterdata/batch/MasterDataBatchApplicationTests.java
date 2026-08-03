package com.ho.account.masterdata.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

/**
 * Proves that the Batch executable can assemble core services without the API
 * module. This is the key runtime boundary introduced by the Gradle split.
 */
@SpringBootTest(properties = {
        "spring.main.web-application-type=none",
        "spring.batch.job.enabled=false",
        "spring.cloud.vault.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "spring.config.on-not-found=ignore",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class MasterDataBatchApplicationTests {

    @Autowired
    private Environment environment;

    @Test
    void contextLoadsWithoutApiModule() {
        assertThat(environment.getProperty("spring.application.name"))
                .isEqualTo("master-data-batch");
        assertThat(environment.getDefaultProfiles()).contains("local");
        assertThat(environment.getProperty("spring.flyway.locations"))
                .isEqualTo("classpath:db/migration");
        assertThat(environment.getProperty("spring.batch.job.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.cloud.config.enabled", Boolean.class)).isFalse();
    }
}
