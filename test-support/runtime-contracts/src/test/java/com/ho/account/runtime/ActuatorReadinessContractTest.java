package com.ho.account.runtime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        classes = ActuatorReadinessContractTest.TestApplication.class,
        properties = {
            "management.endpoint.health.probes.enabled=true",
            "management.endpoints.web.exposure.include=health",
            "spring.cloud.config.enabled=false",
            "spring.cloud.discovery.enabled=false",
            "spring.cloud.vault.enabled=false",
            "eureka.client.enabled=false",
            "spring.autoconfigure.exclude="
                    + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                    + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
        })
@AutoConfigureMockMvc
class ActuatorReadinessContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void readinessEndpointIsAvailableWithoutAnExternalRuntime() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
