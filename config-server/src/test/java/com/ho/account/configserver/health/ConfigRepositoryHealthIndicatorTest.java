package com.ho.account.configserver.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;

class ConfigRepositoryHealthIndicatorTest {

    private static final ConfigRepositoryProbeProperties PROBE =
            new ConfigRepositoryProbeProperties("master-data", "default", null);

    @Test
    void reportsUpWhenRepresentativeConfigurationHasAtLeastOnePropertySource() {
        Environment environment = new Environment("master-data", "default");
        environment.add(new PropertySource("test-source", Map.of("server.port", 8089)));

        Health health = indicator((application, profile, label) -> environment).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("application", "master-data")
                .containsEntry("profile", "default")
                .containsEntry("sourceCount", 1);
    }

    @Test
    void reportsDownWhenRepositoryReturnsAnEmptyEnvironment() {
        Environment empty = new Environment("master-data", "default");

        Health health = indicator((application, profile, label) -> empty).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsEntry("reason", "representative property source is missing")
                .doesNotContainKey("error");
    }

    @Test
    void reportsDownWithoutLeakingRepositoryExceptionDetails() {
        Health health = indicator((application, profile, label) -> {
            throw new IllegalStateException("repository unavailable at file:/sensitive/path");
        }).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsEntry("reason", "repository lookup failed")
                .doesNotContainValue("repository unavailable at file:/sensitive/path")
                .doesNotContainKey("error");
    }

    private ConfigRepositoryHealthIndicator indicator(EnvironmentRepository repository) {
        return new ConfigRepositoryHealthIndicator(repository, PROBE);
    }
}