package com.ho.account.internalaudit.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class InternalAuditRuntimePolicyTest {

    @Test
    void localProfileUsesFlywayOwnedH2WithoutControlPlaneDependencies() {
        try (ConfigurableApplicationContext context = context("local")) {
            Environment environment = context.getEnvironment();
            assertThat(environment.getProperty("spring.datasource.url"))
                    .startsWith("jdbc:h2:mem:")
                    .contains("MODE=PostgreSQL");
            assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class)).isTrue();
            assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                    .isEqualTo("validate");
            assertThat(environment.getProperty("spring.cloud.config.enabled", Boolean.class))
                    .isFalse();
            assertThat(environment.getProperty("eureka.client.enabled", Boolean.class)).isFalse();
        }
    }

    @Test
    void developmentAndProductionUseInjectedPostgresqlWithoutRuntimeSchemaWrites() {
        for (String profile : new String[] {"dev", "prod"}) {
            try (ConfigurableApplicationContext context = context(profile)) {
                Environment environment = context.getEnvironment();
                assertThat(environment.getProperty("spring.datasource.url"))
                        .startsWith("jdbc:postgresql://");
                assertThat(environment.getProperty("spring.datasource.username"))
                        .isEqualTo(profile + "_internal_audit_app");
                assertThat(environment.getProperty("spring.datasource.password"))
                        .isEqualTo("injected-test-password");
                assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class))
                        .isFalse();
                assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                        .isEqualTo("validate");
                assertThat(environment.getProperty("spring.sql.init.mode")).isEqualTo("never");
            }
        }
    }

    @Test
    void onlyApiIsExecutableAndComposeUsesTheCanonicalContainerContract() throws Exception {
        Path root = repositoryRoot();
        String settings = Files.readString(root.resolve("settings.gradle"));
        String coreBuild = Files.readString(root.resolve("internal-audit/core/build.gradle"));
        String compose = Files.readString(root.resolve("internal-audit/docker-compose.yml"));
        Path coreSource = root.resolve("internal-audit/core/src/main/java/com/ho/account");

        assertThat(settings)
                .contains("'internal-audit:core', 'internal-audit:api'")
                .doesNotContain("internal-audit:batch", "auth:batch");
        assertThat(coreBuild)
                .contains("id 'java-library'")
                .doesNotContain("id 'org.springframework.boot'", "spring-boot-starter-web");
        assertThat(coreSource.resolve("internalaudit/InternalAuditApplication.java")).doesNotExist();
        assertThat(javaSources(coreSource.resolve("internalaudit/core/infrastructure/api"))).isEmpty();
        assertThat(javaSources(coreSource.resolve("audit"))).isEmpty();
        assertThat(javaSources(coreSource.resolve("security"))).isEmpty();
        assertThat(root.resolve(
                        "internal-audit/api/src/main/java/com/ho/account/internalaudit/api/adapter/in/web/RcmController.java"))
                .exists();
        assertThat(compose)
                .contains("GRADLE_PROJECT: \":internal-audit:api\"")
                .contains("JAR_DIRECTORY: internal-audit/api")
                .contains("${INTERNAL_AUDIT_DB_URL:?set INTERNAL_AUDIT_DB_URL}")
                .contains("name: ${ACCOUNT_NETWORK_NAME:-account-dev-network}")
                .doesNotContain("internal-audit-batch", "jdbc:h2:");
    }

    private ConfigurableApplicationContext context(String profile) {
        Map<String, Object> overrides = new LinkedHashMap<>();
        overrides.put("spring.profiles.active", profile);
        overrides.put("spring.cloud.config.enabled", false);
        overrides.put("spring.cloud.discovery.enabled", false);
        overrides.put("eureka.client.enabled", false);
        overrides.put("DEV_DB_HOST", "dev-db.invalid");
        overrides.put("DEV_DB_NAME", "internal_audit_dev");
        overrides.put("DEV_DB_USER", "dev_internal_audit_app");
        overrides.put("DEV_DB_PASSWORD", "injected-test-password");
        overrides.put(
                "PROD_DB_URL",
                "jdbc:postgresql://prod-db.invalid:5432/internal_audit_prod?sslmode=verify-full");
        overrides.put("PROD_DB_USER", "prod_internal_audit_app");
        overrides.put("PROD_DB_PASSWORD", "injected-test-password");
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("testOverrides", overrides));

        return new SpringApplicationBuilder(Probe.class)
                .environment(environment)
                .web(WebApplicationType.NONE)
                .registerShutdownHook(false)
                .properties(
                        "spring.config.location="
                                + repositoryRoot()
                                        .resolve("internal-audit/api/src/main/resources")
                                        .toUri(),
                        "spring.main.banner-mode=off")
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

    private List<Path> javaSources(Path root) throws Exception {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (var paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class Probe {}
}
