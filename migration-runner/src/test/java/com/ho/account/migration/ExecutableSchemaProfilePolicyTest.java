package com.ho.account.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

class ExecutableSchemaProfilePolicyTest {

    private static final List<String> EXECUTABLE_RESOURCE_DIRECTORIES = List.of(
            "asset-lease/api/src/main/resources",
            "asset-lease/batch/src/main/resources",
            "auth/api/src/main/resources",
            "auth/batch/src/main/resources",
            "budget/api/src/main/resources",
            "budget/batch/src/main/resources",
            "closing/api/src/main/resources",
            "closing/batch/src/main/resources",
            "deposit/api/src/main/resources",
            "deposit/batch/src/main/resources",
            "loan/api/src/main/resources",
            "loan/batch/src/main/resources",
            "reporting/api/src/main/resources",
            "reporting/batch/src/main/resources");

    @Test
    void everyReadyExecutableLoadsFailClosedDevelopmentAndProductionSchemaPolicies() {
        Path repositoryRoot = repositoryRoot();
        for (String resourceDirectory : EXECUTABLE_RESOURCE_DIRECTORIES) {
            for (String profile : List.of("dev", "prod")) {
                Path location = repositoryRoot.resolve(resourceDirectory);
                try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Probe.class)
                        .web(WebApplicationType.NONE)
                        .registerShutdownHook(false)
                        .properties(
                                "spring.config.location=" + location.toUri(),
                                "spring.profiles.active=" + profile,
                                "spring.main.banner-mode=off",
                                "DEV_DB_HOST=dev-db.invalid",
                                "DEV_DB_PORT=5432",
                                "DEV_DB_NAME=context_dev",
                                "DEV_DB_USER=context_dev_app",
                                "DEV_DB_PASSWORD=injected-dev-secret",
                                "PROD_DB_URL=jdbc:postgresql://prod-db.invalid:5432/context_prod?sslmode=verify-full",
                                "PROD_DB_USER=context_prod_app",
                                "PROD_DB_PASSWORD=injected-prod-secret")
                        .run()) {
                    assertPolicy(context.getEnvironment(), resourceDirectory, profile);
                }
            }
        }
    }

    private void assertPolicy(Environment environment, String executable, String profile) {
        String description = executable + " [" + profile + "]";
        if ("dev".equals(profile)) {
            assertThat(environment.getProperty("spring.datasource.url"))
                    .as(description + " PostgreSQL URL")
                    .isEqualTo("jdbc:postgresql://dev-db.invalid:5432/context_dev");
            assertThat(environment.getProperty("spring.datasource.username"))
                    .isEqualTo("context_dev_app");
            assertThat(environment.getProperty("spring.datasource.password"))
                    .isEqualTo("injected-dev-secret");
        } else {
            assertThat(environment.getProperty("spring.datasource.url"))
                    .as(description + " PostgreSQL URL")
                    .isEqualTo("jdbc:postgresql://prod-db.invalid:5432/context_prod?sslmode=verify-full");
            assertThat(environment.getProperty("spring.datasource.username"))
                    .isEqualTo("context_prod_app");
            assertThat(environment.getProperty("spring.datasource.password"))
                    .isEqualTo("injected-prod-secret");
        }
        assertThat(environment.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("org.postgresql.Driver");
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                .as(description + " JPA")
                .isEqualTo("validate");
        assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class))
                .as(description + " Flyway")
                .isFalse();
        assertThat(environment.getProperty("spring.flyway.clean-disabled", Boolean.class))
                .isTrue();
        assertThat(environment.getProperty("spring.flyway.baseline-on-migrate", Boolean.class))
                .isFalse();
        assertThat(environment.getProperty("spring.batch.jdbc.initialize-schema"))
                .as(description + " Batch metadata")
                .isEqualTo("never");
        assertThat(environment.getProperty("spring.sql.init.mode"))
                .isEqualTo("never");
        if (executable.startsWith("asset-lease/") || executable.startsWith("closing/")) {
            assertThat(environment.getProperty("spring.cloud.config.enabled", Boolean.class))
                    .as(description + " Config Client")
                    .isFalse();
            assertThat(environment.getProperty("spring.cloud.vault.enabled", Boolean.class))
                    .as(description + " Vault")
                    .isFalse();
        }
    }

    @Test
    void localProfilesDisableExternalControlPlaneAndBatchAutorun() {
        Path repositoryRoot = repositoryRoot();
        for (String resourceDirectory : List.of(
                "asset-lease/api/src/main/resources",
                "asset-lease/batch/src/main/resources",
                "closing/api/src/main/resources",
                "closing/batch/src/main/resources")) {
            Path location = repositoryRoot.resolve(resourceDirectory);
            try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Probe.class)
                    .web(WebApplicationType.NONE)
                    .registerShutdownHook(false)
                    .properties(
                            "spring.config.location=" + location.toUri(),
                            "spring.profiles.active=local",
                            "spring.main.banner-mode=off")
                    .run()) {
                Environment environment = context.getEnvironment();
                assertThat(environment.getProperty("spring.cloud.config.enabled", Boolean.class)).isFalse();
                assertThat(environment.getProperty("spring.cloud.discovery.enabled", Boolean.class)).isFalse();
                assertThat(environment.getProperty("spring.cloud.vault.enabled", Boolean.class)).isFalse();
                assertThat(environment.getProperty("eureka.client.enabled", Boolean.class)).isFalse();
                if (resourceDirectory.contains("/batch/")) {
                    assertThat(environment.getProperty("spring.batch.job.enabled", Boolean.class)).isFalse();
                }
            }
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
}
