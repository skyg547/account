package com.ho.account.masterdata;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Proves the HTTP executable assembles core without loading the Batch app.
 * The local profile keeps this boundary test deterministic by selecting the
 * console logger instead of attempting an external Logstash connection.
 */
@SpringBootTest(properties = {
    "spring.main.web-application-type=none",
    "spring.cloud.vault.enabled=false",
    "spring.profiles.active=local",
    "spring.cloud.config.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "eureka.client.enabled=false",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class MasterDataApplicationTests {

    @Test
    void contextLoads() {
    }

}
