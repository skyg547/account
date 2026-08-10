package com.ho.account.budget.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

class BudgetBatchRuntimePolicyTest {

    @Test
    void localProfileUsesFlywayOwnedH2AndNeverAutorunsAJob() {
        try (ConfigurableApplicationContext context = context("local")) {
            Environment environment = context.getEnvironment();
            assertThat(environment.getProperty("spring.datasource.url")).startsWith("jdbc:h2:mem:");
            assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class)).isTrue();
            assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
            assertThat(environment.getProperty("spring.batch.job.enabled", Boolean.class)).isFalse();
            assertThat(environment.getProperty("spring.cloud.config.enabled", Boolean.class)).isFalse();
            assertThat(environment.getProperty("spring.cloud.discovery.enabled", Boolean.class)).isFalse();
        }
    }

    @Test
    void developmentAndProductionProfilesDisableEveryRuntimeSchemaOwnerAndJobAutorun() {
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
                assertThat(environment.getProperty("spring.batch.job.enabled", Boolean.class)).isFalse();
            }
        }
    }

    private ConfigurableApplicationContext context(String profile) {
        return new SpringApplicationBuilder(Probe.class)
                .web(WebApplicationType.NONE)
                .registerShutdownHook(false)
                .properties(
                        "spring.config.location="
                                + repositoryRoot()
                                        .resolve("budget/batch/src/main/resources")
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
