package com.ho.account.reconciliation.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

class ReconciliationLocalHealthPolicyTest {

    @Test
    void redisHealthAndReadinessOverridesAreLocalOnly() {
        assertThat(healthPolicy("local"))
                .isEqualTo(new HealthPolicy(false, true));
        assertThat(healthPolicy("dev"))
                .isEqualTo(new HealthPolicy(null, null));
        assertThat(healthPolicy("prod"))
                .isEqualTo(new HealthPolicy(null, null));
    }

    private HealthPolicy healthPolicy(String profile) {
        Path resources = repositoryRoot().resolve("reconciliation/api/src/main/resources");
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Probe.class)
                .web(WebApplicationType.NONE)
                .registerShutdownHook(false)
                .properties(
                        "spring.config.location=" + resources.toUri(),
                        "spring.profiles.active=" + profile,
                        "spring.main.banner-mode=off")
                .run()) {
            return new HealthPolicy(
                    context.getEnvironment()
                            .getProperty("management.health.redis.enabled", Boolean.class),
                    context.getEnvironment()
                            .getProperty("management.endpoint.health.probes.enabled", Boolean.class));
        }
    }

    private Path repositoryRoot() {
        Path candidate = Path.of("").toAbsolutePath();
        while (candidate != null && !Files.exists(candidate.resolve("settings.gradle"))) {
            candidate = candidate.getParent();
        }
        if (candidate == null) {
            throw new IllegalStateException("repository root was not found");
        }
        return candidate;
    }

    @Configuration(proxyBeanMethods = false)
    static class Probe {
    }

    private record HealthPolicy(Boolean redisHealthEnabled, Boolean probesEnabled) {
    }
}
