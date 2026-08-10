package com.ho.account.budget.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

class BudgetApiRuntimePolicyTest {

    @Test
    void localProfileUsesFlywayOwnedH2AndKeepsControlPlaneOptIn() {
        try (ConfigurableApplicationContext context = context("local")) {
            Environment environment = context.getEnvironment();
            assertThat(environment.getProperty("spring.datasource.url")).startsWith("jdbc:h2:mem:");
            assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class)).isTrue();
            assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
            assertThat(environment.getProperty("spring.cloud.config.enabled", Boolean.class)).isFalse();
            assertThat(environment.getProperty("spring.cloud.discovery.enabled", Boolean.class)).isFalse();
            assertThat(environment.getProperty("eureka.client.enabled", Boolean.class)).isFalse();
        }
    }

    @Test
    void developmentAndProductionProfilesUseInjectedPostgresqlWithoutRuntimeSchemaWrites() {
        for (String profile : new String[] {"dev", "prod"}) {
            try (ConfigurableApplicationContext context = context(profile)) {
                Environment environment = context.getEnvironment();
                assertThat(environment.getProperty("spring.datasource.url"))
                        .startsWith("jdbc:postgresql://");
                assertThat(environment.getProperty("spring.datasource.username"))
                        .isEqualTo(profile + "_budget_app");
                assertThat(environment.getProperty("spring.datasource.password"))
                        .isEqualTo("injected-test-password");
                assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class)).isFalse();
                assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                        .isEqualTo("validate");
                assertThat(environment.getProperty("spring.sql.init.mode")).isEqualTo("never");
                assertThat(environment.getProperty("spring.batch.jdbc.initialize-schema"))
                        .isEqualTo("never");
            }
        }
    }

    @Test
    void composeUsesCanonicalImagesRequiredSecretsAndOptInBatchWithoutBatchIngress() throws Exception {
        String compose = Files.readString(repositoryRoot().resolve("budget/docker-compose.yml"));

        assertThat(compose)
                .contains("GRADLE_PROJECT: \":budget:api\"")
                .contains("JAR_DIRECTORY: budget/api")
                .contains("GRADLE_PROJECT: \":budget:batch\"")
                .contains("JAR_DIRECTORY: budget/batch")
                .contains("${BUDGET_DB_URL:?set BUDGET_DB_URL}")
                .contains("${BUDGET_DB_USER:?set BUDGET_DB_USER}")
                .contains("${BUDGET_DB_PASSWORD:?set BUDGET_DB_PASSWORD}")
                .contains("${AUTH_JWT_SECRET:?set AUTH_JWT_SECRET}")
                .contains("name: ${ACCOUNT_NETWORK_NAME:-account-dev-network}")
                .contains("profiles: [\"batch\"]")
                .contains("SPRING_BATCH_JOB_ENABLED: \"false\"")
                .contains("${BUDGET_API_PORT:-8096}:8096")
                .doesNotContain("jdbc:postgresql://");
        String batch = compose.substring(compose.indexOf("  budget-batch:"));
        assertThat(batch).doesNotContain("ports:");
    }

    private ConfigurableApplicationContext context(String profile) {
        return new SpringApplicationBuilder(Probe.class)
                .web(WebApplicationType.NONE)
                .registerShutdownHook(false)
                .properties(
                        "spring.config.location="
                                + repositoryRoot()
                                        .resolve("budget/api/src/main/resources")
                                        .toUri(),
                        "spring.profiles.active=" + profile,
                        "spring.main.banner-mode=off",
                        "DEV_DB_HOST=dev-db.invalid",
                        "DEV_DB_NAME=budget_dev",
                        "DEV_DB_USER=dev_budget_app",
                        "DEV_DB_PASSWORD=injected-test-password",
                        "PROD_DB_URL=jdbc:postgresql://prod-db.invalid:5432/budget_prod?sslmode=verify-full",
                        "PROD_DB_USER=prod_budget_app",
                        "PROD_DB_PASSWORD=injected-test-password")
                .run();
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
    static class Probe {}
}
