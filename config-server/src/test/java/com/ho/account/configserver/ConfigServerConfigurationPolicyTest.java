package com.ho.account.configserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.yaml.snakeyaml.Yaml;

class ConfigServerConfigurationPolicyTest {

    @Test
    void developmentAndProductionDelegateAllSchemaChangesToTheReleaseMigrator() {
        for (String profile : List.of("dev", "prod")) {
            Properties properties = loadProperties(resolveFromRepositoryRoot(
                    "config-repo", "application-" + profile + ".yml"));

            if ("dev".equals(profile)) {
                assertThat(properties.getProperty("spring.datasource.url"))
                        .isEqualTo("jdbc:postgresql://${DEV_DB_HOST}:${DEV_DB_PORT:5432}/${DEV_DB_NAME}");
                assertThat(properties.getProperty("spring.datasource.username"))
                        .isEqualTo("${DEV_DB_USER}");
                assertThat(properties.getProperty("spring.datasource.password"))
                        .isEqualTo("${DEV_DB_PASSWORD}");
            } else {
                assertThat(properties.getProperty("spring.datasource.url"))
                        .isEqualTo("${PROD_DB_URL}");
                assertThat(properties.getProperty("spring.datasource.username"))
                        .isEqualTo("${PROD_DB_USER}");
                assertThat(properties.getProperty("spring.datasource.password"))
                        .isEqualTo("${PROD_DB_PASSWORD}");
            }
            assertThat(properties.getProperty("spring.datasource.driver-class-name"))
                    .isEqualTo("org.postgresql.Driver");
            assertThat(properties.getProperty("spring.jpa.hibernate.ddl-auto"))
                    .as(profile + " JPA schema policy")
                    .isEqualTo("validate");
            assertThat(properties.getProperty("spring.flyway.enabled"))
                    .as(profile + " startup Flyway policy")
                    .isEqualTo("false");
            assertThat(properties.getProperty("spring.flyway.clean-disabled"))
                    .isEqualTo("true");
            assertThat(properties.getProperty("spring.flyway.baseline-on-migrate"))
                    .isEqualTo("false");
            assertThat(properties.getProperty("spring.batch.jdbc.initialize-schema"))
                    .as(profile + " Batch metadata policy")
                    .isEqualTo("never");
            assertThat(properties.getProperty("spring.sql.init.mode"))
                    .isEqualTo("never");
        }
    }

    @Test
    void localDefaultsUseNativeRepositoryAndRepositoryAwareReadiness() {
        Properties properties = loadProperties(resolveFromRepositoryRoot(
                "config-server", "src", "main", "resources", "application.yml"));

        assertThat(properties.getProperty("server.port")).isEqualTo("${SERVER_PORT:8888}");
        assertThat(properties.getProperty("encrypt.key")).isEqualTo("${ENCRYPT_KEY:account-config-server-secret-key}");
        assertThat(properties.getProperty("spring.profiles.active"))
                .isEqualTo("${SPRING_PROFILES_ACTIVE:native}");
        assertThat(properties.getProperty("spring.cloud.config.server.native.search-locations"))
                .isEqualTo("${CONFIG_REPO_LOCATION:file:./config-repo}");
        assertThat(properties.getProperty("spring.cloud.config.server.health.repositories.master-data.name"))
                .isEqualTo("master-data");
        assertThat(properties.getProperty("config-server.repository-probe.application"))
                .isEqualTo("master-data");
        assertThat(properties.getProperty("management.endpoint.health.group.readiness.include"))
                .isEqualTo("readinessState,configRepository");
    }

    @Test
    void productionProfileUsesGitBackendAndPropertyEncryption() {
        Properties prodProperties = loadProperties(resolveFromRepositoryRoot(
                "config-server", "src", "main", "resources", "application-prod.yml"));

        assertThat(prodProperties.getProperty("spring.cloud.config.server.git.uri"))
                .isEqualTo("${CONFIG_GIT_URI:https://github.com/skyg547/account-config-repo.git}");
        assertThat(prodProperties.getProperty("spring.cloud.config.server.git.default-label"))
                .isEqualTo("${CONFIG_GIT_LABEL:main}");
        assertThat(prodProperties.getProperty("spring.cloud.config.server.git.search-paths"))
                .isEqualTo("${CONFIG_GIT_SEARCH_PATHS:*}");
        assertThat(prodProperties.getProperty("spring.cloud.config.server.git.clone-on-start"))
                .isEqualTo("${CONFIG_GIT_CLONE_ON_START:true}");
    }



    @Test
    void moduleComposeUsesRootBuildContextAndReadOnlyRepositoryMount() throws IOException {
        Map<String, Object> root = loadYaml(resolveFromRepositoryRoot("config-server", "docker-compose.yml"));
        Map<String, Object> configServer = asMap(asMap(root.get("services")).get("config-server"));
        Map<String, Object> build = asMap(configServer.get("build"));

        assertThat(build.get("context")).isEqualTo("..");
        assertThat(build.get("dockerfile")).isEqualTo("config-server/Dockerfile");
        assertThat(build).doesNotContainKey("args");
        assertThat(asList(configServer.get("volumes"))).contains("../config-repo:/config-repo:ro");
        assertThat(asList(configServer.get("environment")))
                .contains("SPRING_PROFILES_ACTIVE=native", "CONFIG_REPO_LOCATION=file:/config-repo");
    }

    @Test
    void developmentProfileRequiresPostgreSqlWithoutH2Fallback() throws IOException {
        Path profile = resolveFromRepositoryRoot("config-repo", "application-dev.yml");
        Properties properties = loadProperties(profile);
        String yaml = Files.readString(profile);

        assertThat(properties.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://${DEV_DB_HOST}:${DEV_DB_PORT:5432}/${DEV_DB_NAME}");
        assertThat(properties.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("org.postgresql.Driver");
        assertThat(properties.getProperty("spring.datasource.username")).isEqualTo("${DEV_DB_USER}");
        assertThat(properties.getProperty("spring.datasource.password")).isEqualTo("${DEV_DB_PASSWORD}");
        assertThat(properties.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(properties.getProperty("spring.flyway.enabled")).isEqualTo("false");
        assertThat(properties.getProperty("spring.sql.init.mode")).isEqualTo("never");
        assertThat(yaml)
                .doesNotContain("jdbc:h2:")
                .doesNotContain("ddl-auto: update")
                .doesNotContain("dev_pass");
    }

    @Test
    void developmentPostgresComposeUsesBootstrapHealthAndSecretReferences() throws IOException {
        Path composePath = resolveFromRepositoryRoot("postgres", "docker-compose.yml");
        Map<String, Object> root = loadYaml(composePath);
        Map<String, Object> services = asMap(root.get("services"));
        Map<String, Object> postgres = asMap(services.get("postgres-db"));
        Map<String, Object> environment = asMap(postgres.get("environment"));
        Map<String, Object> healthcheck = asMap(postgres.get("healthcheck"));
        Map<String, Object> pgadmin = asMap(services.get("pgadmin"));
        Map<String, Object> pgadminDependency =
                asMap(asMap(pgadmin.get("depends_on")).get("postgres-db"));
        String compose = Files.readString(composePath);
        String initScript = Files.readString(resolveFromRepositoryRoot(
                "postgres", "init", "10-create-service-databases.sh"));
        String runtimeGrantScript = Files.readString(resolveFromRepositoryRoot(
                "postgres", "runtime", "grant-runtime-privileges.sh"));
        Path exampleEnvironmentPath = resolveFromRepositoryRoot(".env.example");
        String exampleEnvironment = Files.readString(exampleEnvironmentPath);
        Properties exampleProperties = loadKeyValueProperties(exampleEnvironmentPath);

        assertThat(environment.get("POSTGRES_USER").toString())
                .contains("${POSTGRES_ADMIN_USER:?");
        assertThat(environment.get("POSTGRES_PASSWORD").toString())
                .contains("${POSTGRES_ADMIN_PASSWORD:?");
        assertThat(environment.get("ACCOUNT_DATABASES").toString())
                .contains("${ACCOUNT_DATABASES:?");
        assertThat(environment.get("ACCOUNT_DB_OWNER_PASSWORD").toString())
                .contains("${ACCOUNT_DB_OWNER_PASSWORD:?");
        assertThat(environment.get("ACCOUNT_DB_APP_PASSWORD").toString())
                .contains("${ACCOUNT_DB_APP_PASSWORD:?");
        assertThat(asList(postgres.get("profiles"))).contains("self-contained-db");
        assertThat(asList(healthcheck.get("test")).toString())
                .contains("pg_isready")
                .contains("account_dev_bootstrap_status")
                .contains("ACCOUNT_DATABASES");
        assertThat(asList(postgres.get("volumes")))
                .contains(
                        "./init:/docker-entrypoint-initdb.d:ro",
                        "./runtime:/account-runtime:ro");
        assertThat(asList(pgadmin.get("profiles"))).contains("admin");
        assertThat(pgadminDependency.get("condition")).isEqualTo("service_healthy");

        assertThat(initScript)
                .contains("CREATE ROLE %I LOGIN PASSWORD %L")
                .contains("CREATE DATABASE %I OWNER %I")
                .contains("ALTER SCHEMA public OWNER TO %I")
                .contains("GRANT CONNECT ON DATABASE %I TO %I")
                .contains("GRANT USAGE ON SCHEMA public TO %I")
                .contains("Development owner and runtime passwords must differ")
                .contains("account_dev_bootstrap_status")
                .contains("database_manifest")
                .doesNotContain("ALTER DEFAULT PRIVILEGES")
                .doesNotContain("set -x");
        assertThat(runtimeGrantScript)
                .contains(
                        "GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE %I.%I TO %I",
                        "GRANT USAGE, SELECT ON SEQUENCE %I.%I TO %I",
                        "REVOKE ALL PRIVILEGES ON TABLE %I.%I FROM %I",
                        "tablename LIKE 'flyway\\_schema\\_history%' ESCAPE '\\'")
                .doesNotContain("set -x", "ALTER DEFAULT PRIVILEGES");
        assertThat(exampleEnvironment)
                .contains("ACCOUNT_DATABASES=auth_dev,master_data_dev")
                .contains("POSTGRES_ADMIN_PASSWORD=replace-with-local-admin-password")
                .contains("ACCOUNT_DB_OWNER_PASSWORD=replace-with-local-owner-password")
                .contains("ACCOUNT_DB_APP_PASSWORD=replace-with-local-app-password");
        assertThat(exampleProperties.getProperty("ACCOUNT_DATABASES").split(","))
                .containsExactly(
                        "auth_dev",
                        "master_data_dev",
                        "internal_audit_dev",
                        "budget_dev",
                        "journal_ledger_dev",
                        "closing_dev",
                        "loan_dev",
                        "deposit_dev",
                        "asset_lease_dev",
                        "payable_dev",
                        "receivable_dev",
                        "reconciliation_dev",
                        "tax_dev",
                        "expenditure_resolution_dev",
                        "reporting_dev",
                        "account_mart_dev",
                        "ecl_dev");
        assertThat(compose)
                .doesNotContain("POSTGRES_PASSWORD: postgres")
                .doesNotContain("PGADMIN_DEFAULT_PASSWORD: admin")
                .doesNotContain("dev_pass");
    }

    @Test
    void externalDevelopmentComposeUsesAuthenticatedProbeWithoutLocalDatabase() throws IOException {
        Path composePath = resolveFromRepositoryRoot("postgres", "compose.external-dev.yml");
        Map<String, Object> root = loadYaml(composePath);
        Map<String, Object> services = asMap(root.get("services"));
        Map<String, Object> probe = asMap(services.get("external-dev-db-check"));
        Map<String, Object> environment = asMap(probe.get("environment"));
        Map<String, Object> healthcheck = asMap(probe.get("healthcheck"));
        String compose = Files.readString(composePath);
        String exampleEnvironment = Files.readString(resolveFromRepositoryRoot(
                ".env.external-dev.example"));
        String gitignore = Files.readString(resolveFromRepositoryRoot(".gitignore"));

        assertThat(services).doesNotContainKey("postgres-db");
        assertThat(asList(probe.get("profiles"))).contains("external-dev");
        assertThat(environment.get("DEV_DB_HOST").toString()).contains("${DEV_DB_HOST:?");
        assertThat(environment.get("DEV_DB_NAME").toString()).contains("${DEV_DB_NAME:?");
        assertThat(environment.get("DEV_DB_USER").toString()).contains("${DEV_DB_USER:?");
        assertThat(environment.get("DEV_DB_PASSWORD").toString()).contains("${DEV_DB_PASSWORD:?");
        assertThat(asList(healthcheck.get("test")).toString())
                .contains("PGPASSWORD=\"$$DEV_DB_PASSWORD\"")
                .contains("--command \"SELECT 1\"");
        assertThat(probe).doesNotContainKeys("ports", "volumes");
        assertThat(exampleEnvironment)
                .contains("DEV_DB_HOST=replace-with-approved-shared-host")
                .contains("DEV_DB_PASSWORD=replace-with-secret-provider-value");
        assertThat(gitignore).contains(".env.external-dev");
        assertThat(compose).doesNotContain("postgres-db:");
    }

    @Test
    void dockerAndIntellijPoliciesUseJava17BootJarRepositoryAndReadiness() throws IOException {
        String dockerfile = Files.readString(resolveFromRepositoryRoot("config-server", "Dockerfile"));
        assertThat(dockerfile)
                .contains("gradle:8.7-jdk17-alpine")
                .contains("eclipse-temurin:17-jre-alpine")
                .contains(":config-server:bootJar")
                .contains("ENV CONFIG_REPO_LOCATION=file:/config-repo")
                .contains("/actuator/health/readiness")
                .doesNotContain("COPY --from=builder /build/config-repo")
                .doesNotContain("|| true");

        String runConfiguration = Files.readString(resolveFromRepositoryRoot(
                ".run", "Config Server bootRun.run.xml"));
        assertThat(runConfiguration)
                .contains(":config-server:bootRun")
                .contains("--spring.profiles.active=native")
                .contains("--spring.cloud.config.server.native.search-locations=file:./config-repo")
                .contains("--no-daemon");
    }

    private Properties loadProperties(Path path) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new FileSystemResource(path));
        Properties properties = factory.getObject();
        if (properties == null) {
            throw new IllegalStateException("YAML properties could not be loaded: " + path);
        }
        return properties;
    }

    private Properties loadKeyValueProperties(Path path) throws IOException {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(path)) {
            properties.load(reader);
        }
        return properties;
    }

    private Map<String, Object> loadYaml(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return asMap(new Yaml().load(inputStream));
        }
    }

    private Path resolveFromRepositoryRoot(String... pathParts) {
        Path root = Files.exists(Path.of("settings.gradle")) ? Path.of("") : Path.of("..");
        Path resolved = root;
        for (String pathPart : pathParts) {
            resolved = resolved.resolve(pathPart);
        }
        if (!Files.exists(resolved)) {
            throw new IllegalStateException("configuration file was not found: " + resolved);
        }
        return resolved;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("expected YAML map but got: " + value);
        }
        return (Map<String, Object>) map;
    }

    @SuppressWarnings("unchecked")
    private List<Object> asList(Object value) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException("expected YAML list but got: " + value);
        }
        return (List<Object>) list;
    }
}
