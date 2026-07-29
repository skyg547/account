package com.ho.account.masterdata.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

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
    void contextLoadsWithoutApiModule() throws IOException {
        assertThat(environment.getProperty("spring.application.name"))
                .isEqualTo("master-data-batch");
        assertThat(environment.getProperty("spring.cloud.config.name"))
                .isEqualTo("master-data,master-data-batch");

        PropertySource<?> applicationYaml = new YamlPropertySourceLoader()
                .load("application.yml", new ClassPathResource("application.yml"))
                .get(0);
        assertThat(applicationYaml.getProperty("spring.config.import"))
                .isEqualTo("configserver:http://localhost:8888/")
                .asString()
                .doesNotStartWith("optional:");
    }
}
