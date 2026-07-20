package com.ho.account.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInfo;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.register-with-eureka=false",
                "eureka.client.fetch-registry=false",
                "eureka.server.wait-time-in-ms-when-sync-empty=0",
                "management.tracing.enabled=false"
        })
class DiscoveryApplicationTests {

    private static final String TEST_APP = "DISCOVERY-POLICY-TEST";
    private static final String TEST_INSTANCE = "discovery-policy-test:65530";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PeerAwareInstanceRegistry registry;

    @Autowired
    private EurekaClientConfig eurekaClientConfig;

    @Test
    void readinessEndpointReportsUpAfterRegistryInitialization() {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/actuator/health/readiness",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "UP");
        assertThat(registry.shouldAllowAccess(false)).isTrue();
        assertThat(eurekaClientConfig.shouldRegisterWithEureka()).isFalse();
        assertThat(eurekaClientConfig.shouldFetchRegistry()).isFalse();
    }

    @Test
    void registrySupportsRegisterLookupAndCancelLifecycle() {
        InstanceInfo instance = InstanceInfo.Builder.newBuilder()
                .setInstanceId(TEST_INSTANCE)
                .setAppName(TEST_APP)
                .setHostName("localhost")
                .setIPAddr("127.0.0.1")
                .setStatus(InstanceInfo.InstanceStatus.UP)
                .setPort(65530)
                .setVIPAddress("discovery-policy-test")
                .setSecureVIPAddress("discovery-policy-test")
                .setDataCenterInfo(new MyDataCenterInfo(DataCenterInfo.Name.MyOwn))
                .build();

        try {
            registry.register(instance, false);

            InstanceInfo registered = registry.getInstanceByAppAndId(TEST_APP, TEST_INSTANCE);
            assertThat(registered).isNotNull();
            assertThat(registered.getStatus()).isEqualTo(InstanceInfo.InstanceStatus.UP);
        } finally {
            registry.cancel(TEST_APP, TEST_INSTANCE, false);
        }

        assertThat(registry.getInstanceByAppAndId(TEST_APP, TEST_INSTANCE)).isNull();
    }
}
