package com.ho.account.configserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
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
                "spring.profiles.active=native",
                "spring.cloud.config.server.native.search-locations=classpath:/config-repository",
                "spring.cloud.config.server.health.repositories.master-data.name=policy-test-service",
                "spring.cloud.config.server.health.repositories.master-data.profiles=default",
                "config-server.repository-probe.application=policy-test-service",
                "config-server.repository-probe.profile=default",
                "ENCRYPT_KEY=${random.uuid}"
        })
class ConfigServerApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void readinessIsUpWhenRepresentativeConfigurationCanBeRead() {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/actuator/health/readiness",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "UP");
    }

    @Test
    void environmentEndpointServesApplicationConfigurationFromNativeRepository() {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/policy-test-service/default",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("name")).isEqualTo("policy-test-service");
        assertThat(asList(response.getBody().get("profiles"))).containsExactly("default");

        Map<String, Object> properties = mergePropertySources(response.getBody());
        assertThat(properties)
                .containsEntry("policy.owner", "config-server-test")
                .containsEntry("policy.retry.max-attempts", 3);
    }

    private Map<String, Object> mergePropertySources(Map<String, Object> environment) {
        Map<String, Object> merged = new LinkedHashMap<>();
        asList(environment.get("propertySources")).stream()
                .map(this::asMap)
                .map(propertySource -> asMap(propertySource.get("source")))
                // Config Server는 앞쪽 property source의 우선순위가 더 높으므로 최초 값을 보존합니다.
                .forEach(source -> source.forEach(merged::putIfAbsent));
        return merged;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Object> asList(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return (List<Object>) value;
    }
}
