package com.ho.account.configserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

class ProductionComposePolicyTest {

    private static final Set<String> PLATFORM_SERVICES =
            Set.of("config-server", "discovery", "gateway", "frontend");
    private static final Set<String> DOMAIN_PREFIXES = Set.of(
            "account-mart",
            "asset-lease",
            "auth",
            "budget",
            "closing",
            "deposit",
            "ecl",
            "expenditure-resolution",
            "internal-audit",
            "journal-ledger",
            "loan",
            "master-data",
            "payable",
            "receivable",
            "reconciliation",
            "reporting",
            "tax");
    private static final Set<String> BATCH_DOMAIN_PREFIXES = Set.of(
            "account-mart",
            "asset-lease",
            "budget",
            "closing",
            "deposit",
            "ecl",
            "expenditure-resolution",
            "journal-ledger",
            "loan",
            "master-data",
            "payable",
            "receivable",
            "reconciliation",
            "reporting",
            "tax");

    @Test
    void productionComposeIsBuildlessAndOwnsEveryCurrentlyExecutableService()
            throws IOException {
        Path composePath = resolve("compose.prod.yml");
        String composeText = Files.readString(composePath);
        Map<String, Object> root = loadYaml(composePath);
        Map<String, Object> services = asMap(root.get("services"));
        Set<String> expected = new HashSet<>(PLATFORM_SERVICES);
        for (String domain : DOMAIN_PREFIXES) {
            expected.add(domain + "-api");
            if (BATCH_DOMAIN_PREFIXES.contains(domain)) {
                expected.add(domain + "-batch");
            }
        }

        assertThat(services.keySet()).containsExactlyInAnyOrderElementsOf(expected);
        assertThat(services).hasSize(36).doesNotContainKeys(
                "postgres", "postgres-db", "auth-batch", "internal-audit-batch");
        assertThat(composeText)
                .doesNotContain("build:")
                .doesNotContain("jdbc:h2:")
                .doesNotContain("ddl-auto: update")
                .doesNotContain("ddl-auto: create")
                .doesNotContain("POSTGRES_PASSWORD")
                .doesNotContain("pids_limit:")
                .doesNotContain("192.168.");

        for (Map.Entry<String, Object> entry : services.entrySet()) {
            String name = entry.getKey();
            Map<String, Object> service = asMap(entry.getValue());

            assertThat(service).as(name)
                    .doesNotContainKey("build")
                    .doesNotContainKey("pids_limit")
                    .containsKeys("image", "restart", "read_only", "cap_drop",
                            "security_opt", "logging", "deploy");
            assertThat(service.get("image").toString()).as(name)
                    .startsWith("${")
                    .contains("_IMAGE:?set immutable ")
                    .endsWith("_IMAGE digest}");
            assertThat(service.get("read_only")).as(name).isEqualTo(true);
            assertThat(asList(service.get("cap_drop"))).as(name).contains("ALL");
            assertThat(asList(service.get("security_opt"))).as(name)
                    .contains("no-new-privileges:true");

            Map<String, Object> deploy = asMap(service.get("deploy"));
            Map<String, Object> resources = asMap(deploy.get("resources"));
            Map<String, Object> limits = asMap(resources.get("limits"));
            assertThat(limits).as(name).containsKey("pids");
            int pids = Integer.parseInt(limits.get("pids").toString());
            if (name.endsWith("-batch")) {
                assertThat(pids).as(name).isEqualTo(512);
            } else if (Set.of("config-server", "discovery", "frontend").contains(name)) {
                assertThat(pids).as(name).isEqualTo(128);
            } else {
                assertThat(pids).as(name).isEqualTo(256);
            }

            if (name.endsWith("-batch")) {
                assertThat(asList(service.get("profiles"))).containsExactly("batch");
                assertThat(service.get("restart").toString()).isEqualTo("no");
            } else {
                assertThat(asList(service.get("profiles"))).contains("prod");
            }
        }
    }

    @Test
    void onlyGatewayAndFrontendPublishHostPorts() throws IOException {
        Map<String, Object> services =
                asMap(loadYaml(resolve("compose.prod.yml")).get("services"));

        assertThat(asList(asMap(services.get("gateway")).get("ports")))
                .containsExactly("${PROD_GATEWAY_PORT:-8000}:8000");
        assertThat(asList(asMap(services.get("frontend")).get("ports")))
                .containsExactly("${PROD_FRONTEND_PORT:-3000}:3000");
        assertThat(services.entrySet())
                .filteredOn(entry -> !Set.of("gateway", "frontend").contains(entry.getKey()))
                .allSatisfy(entry -> assertThat(asMap(entry.getValue()))
                        .as(entry.getKey())
                        .doesNotContainKey("ports"));
    }

    @Test
    void everyExecutableDomainRequiresItsOwnExternalPostgresqlContract() throws IOException {
        Map<String, Object> services =
                asMap(loadYaml(resolve("compose.prod.yml")).get("services"));

        for (String domain : DOMAIN_PREFIXES) {
            String variablePrefix = domain.replace("-", "_").toUpperCase();
            List<String> runtimes = BATCH_DOMAIN_PREFIXES.contains(domain)
                    ? List.of("api", "batch")
                    : List.of("api");
            for (String runtime : runtimes) {
                String serviceName = domain + "-" + runtime;
                Map<String, Object> environment =
                        asMap(asMap(services.get(serviceName)).get("environment"));
                assertThat(environment).as(serviceName)
                        .containsEntry(
                                "SPRING_DATASOURCE_URL",
                                "${" + variablePrefix + "_DB_URL:?set "
                                        + variablePrefix + "_DB_URL}")
                        .containsEntry(
                                "SPRING_DATASOURCE_USERNAME",
                                "${" + variablePrefix + "_DB_USER:?set "
                                        + variablePrefix + "_DB_USER}")
                        .containsEntry(
                                "SPRING_DATASOURCE_PASSWORD",
                                "${" + variablePrefix + "_DB_PASSWORD:?set "
                                        + variablePrefix + "_DB_PASSWORD}")
                        .containsEntry("SPRING_JPA_HIBERNATE_DDL_AUTO", "validate")
                        .containsEntry("SPRING_FLYWAY_ENABLED", "false")
                        .containsEntry("SPRING_FLYWAY_CLEAN_DISABLED", "true")
                        .containsEntry("SPRING_SQL_INIT_MODE", "never")
                        .containsEntry("SPRING_BATCH_JDBC_INITIALIZE_SCHEMA", "never");
            }
        }
    }

    @Test
    void authGatewayAndBudgetShareOneRequiredJwtAndBudgetIntegrationIsExplicit()
            throws IOException {
        Map<String, Object> services =
                asMap(loadYaml(resolve("compose.prod.yml")).get("services"));
        Map<String, Object> authEnvironment =
                asMap(asMap(services.get("auth-api")).get("environment"));
        Map<String, Object> gatewayEnvironment =
                asMap(asMap(services.get("gateway")).get("environment"));
        Map<String, Object> frontendEnvironment =
                asMap(asMap(services.get("frontend")).get("environment"));
        Map<String, Object> apiEnvironment =
                asMap(asMap(services.get("budget-api")).get("environment"));
        Map<String, Object> batchEnvironment =
                asMap(asMap(services.get("budget-batch")).get("environment"));

        String requiredJwt = "${AUTH_JWT_SECRET:?set AUTH_JWT_SECRET}";
        assertThat(authEnvironment)
                .containsEntry("AUTH_JWT_SECRET", requiredJwt)
                .containsEntry(
                        "AUTH_DEFAULT_PASSWORD",
                        "${AUTH_DEFAULT_PASSWORD:?set AUTH_DEFAULT_PASSWORD}")
                .containsEntry(
                        "AUTH_INTERNAL_API_TOKEN",
                        "${AUTH_INTERNAL_API_TOKEN:?set AUTH_INTERNAL_API_TOKEN}");
        assertThat(gatewayEnvironment)
                .containsEntry("AUTH_JWT_SECRET", requiredJwt)
                .containsEntry(
                        "AUTH_TOKEN_VERSION_VALIDATION_BASE_URL",
                        "http://auth-api:8080");
        assertThat(frontendEnvironment)
                .containsEntry("NODE_ENV", "production")
                .containsEntry("GATEWAY_INTERNAL_URL", "http://gateway:8000")
                .containsEntry("FRONTEND_PUBLIC_ORIGIN", "${FRONTEND_PUBLIC_ORIGIN:-}");
        assertThat(apiEnvironment)
                .containsEntry(
                        "AUTH_JWT_SECRET",
                        requiredJwt)
                .containsEntry("SPRING_CLOUD_CONFIG_ENABLED", "true")
                .containsEntry("SPRING_CLOUD_DISCOVERY_ENABLED", "true")
                .containsEntry("BUDGET_DISCOVERY_ENABLED", "true")
                .containsEntry("BUDGET_EUREKA_ENABLED", "true");
        assertThat(batchEnvironment)
                .containsEntry("SPRING_CLOUD_CONFIG_ENABLED", "true")
                .containsEntry("SPRING_MAIN_WEB_APPLICATION_TYPE", "none")
                .containsEntry(
                        "SPRING_BATCH_JOB_ENABLED", "${PROD_BATCH_JOB_ENABLED:-false}");
    }

    @Test
    void productionConfigFailsClosedOnPostgresqlAndMigrationPolicy() {
        Properties properties = loadProperties(resolve("config-repo", "application-prod.yml"));

        assertThat(properties.getProperty("spring.datasource.url")).isEqualTo("${PROD_DB_URL}");
        assertThat(properties.getProperty("spring.datasource.username")).isEqualTo("${PROD_DB_USER}");
        assertThat(properties.getProperty("spring.datasource.password")).isEqualTo("${PROD_DB_PASSWORD}");
        assertThat(properties.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("org.postgresql.Driver");
        assertThat(properties.getProperty("spring.jpa.database-platform"))
                .isEqualTo("org.hibernate.dialect.PostgreSQLDialect");
        assertThat(properties.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(properties.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
        assertThat(properties.getProperty("spring.flyway.enabled")).isEqualTo("false");
        assertThat(properties.getProperty("spring.flyway.clean-disabled")).isEqualTo("true");
        assertThat(properties.getProperty("spring.flyway.baseline-on-migrate")).isEqualTo("false");
        assertThat(properties).doesNotContainKey("spring.flyway.validate-on-migrate");
        assertThat(properties.getProperty("spring.sql.init.mode")).isEqualTo("never");
        assertThat(properties.getProperty("spring.batch.jdbc.initialize-schema")).isEqualTo("never");
        assertThat(properties.getProperty("spring.kafka.bootstrap-servers"))
                .isEqualTo("${PROD_KAFKA_BOOTSTRAP_SERVERS}");
        assertThat(properties.getProperty("spring.data.redis.host"))
                .isEqualTo("${PROD_REDIS_HOST}");
        assertThat(properties.getProperty("spring.data.redis.port"))
                .isEqualTo("${PROD_REDIS_PORT}");
    }

    @Test
    void productionTemplateUsesOnlyDummyHostsEmptyPasswordsAndDigestImages()
            throws IOException {
        String template = Files.readString(resolve(".env.prod.example"));
        String gitignore = Files.readString(resolve(".gitignore"));
        String dockerignore = Files.readString(resolve(".dockerignore"));
        Pattern digest = Pattern.compile(
                "(?m)^[A-Z0-9_]+_IMAGE=[^\\r\\n]+@sha256:[0-9a-f]{64}$");
        Pattern emptyPassword = Pattern.compile("(?m)^[A-Z0-9_]+_DB_PASSWORD=$");
        Pattern verifiedTlsUrl = Pattern.compile(
                "(?m)^[A-Z0-9_]+_DB_URL=jdbc:postgresql://[^\\r\\n]+\\?sslmode=verify-full$");
        Pattern runtimeUser = Pattern.compile("(?m)^[A-Z0-9_]+_DB_USER=[a-z0-9_]+_app$");

        assertThat(gitignore).contains(".env.prod");
        assertThat(dockerignore).contains(".env.prod");
        assertThat(template)
                .contains("db.example.invalid")
                .contains(
                        "AUTH_JWT_SECRET=",
                        "AUTH_DEFAULT_PASSWORD=",
                        "AUTH_INTERNAL_API_TOKEN=")
                .doesNotContain("192.168.", "localhost", "dev_pass", "password=password");
        assertThat(digest.matcher(template).results()).hasSize(36);
        assertThat(emptyPassword.matcher(template).results()).hasSize(17);
        assertThat(verifiedTlsUrl.matcher(template).results()).hasSize(17);
        assertThat(runtimeUser.matcher(template).results()).hasSize(17);
    }

    @Test
    void environmentValidatorNeverPrintsValuesAndRejectsMutableProductionInputs()
            throws IOException {
        String script = Files.readString(resolve("validate-prod-env.ps1"));

        assertThat(script)
                .contains(
                        "Missing $($missingNames.Count)",
                        "@sha256:[0-9a-f]{64}",
                        "Placeholder image digests are not allowed",
                        "Production database URLs must not use a local-container shortcut",
                        "Host environment overrides",
                        "PROD_BATCH_JOB_ENABLED must be exactly false",
                        "sslmode=verify-full",
                        "Interpolated double-quoted values are forbidden",
                        "Backslash escape sequences are forbidden",
                        "least-privilege runtime database user",
                        "JWT secrets must be at least 32 characters",
                        "AUTH_DEFAULT_PASSWORD does not meet the minimum production policy",
                        "AUTH_INTERNAL_API_TOKEN must be at least 32 characters",
                        "password.Length -lt 16",
                        "Production Compose must not contain source build directives")
                .doesNotContain("Write-Output $values", "Write-Host $values");
    }

    @Test
    void productionRunbookFailsClosedOnServiceReadiness() throws IOException {
        // The docs restructure moved this runbook under docs/guides/. Keep the path in sync
        // here: a stale path makes the test fail on a missing file, which reads as a policy
        // violation and turns config-server red for every unrelated PR touching the module.
        String runbook = Files.readString(resolve("docs", "guides", "production-compose.md"));

        assertThat(runbook)
                .contains(
                        "--profile prod up -d --wait --wait-timeout 300 --no-build --pull never",
                        "prod profile의 21개 서비스가 running/healthy",
                        "#243",
                        "#244")
                .doesNotContain("--profile prod up -d --no-build --pull never");
    }

    @Test
    void environmentValidatorRunsItsNegativeSecurityFixtures() throws Exception {
        Path script = resolve("validate-prod-env.ps1").toAbsolutePath();
        String executable = System.getProperty("os.name").toLowerCase().contains("win")
                ? "powershell.exe"
                : "pwsh";
        Process process;
        try {
            process = new ProcessBuilder(
                            executable,
                            "-NoProfile",
                            "-ExecutionPolicy",
                            "Bypass",
                            "-File",
                            script.toString(),
                            "-SelfTest")
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException unavailablePowerShell) {
            Assumptions.assumeTrue(false, "PowerShell is unavailable for validator self-test");
            return;
        }

        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
        }
        assertThat(finished).isTrue();
        assertThat(process.exitValue()).isZero();
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

    private Map<String, Object> loadYaml(Path path) throws IOException {
        LoaderOptions options = new LoaderOptions();
        options.setMaxAliasesForCollections(200);
        try (InputStream inputStream = Files.newInputStream(path)) {
            return asMap(new Yaml(new SafeConstructor(options)).load(inputStream));
        }
    }

    private Path resolve(String... parts) {
        Path root = Files.exists(Path.of("settings.gradle")) ? Path.of("") : Path.of("..");
        Path resolved = root;
        for (String part : parts) {
            resolved = resolved.resolve(part);
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
