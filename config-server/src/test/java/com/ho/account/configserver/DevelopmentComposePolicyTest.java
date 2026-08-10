package com.ho.account.configserver;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

class DevelopmentComposePolicyTest {

    private static final Set<String> PLATFORM_SERVICES =
            Set.of("config-server", "discovery", "gateway", "frontend");
    private static final Set<String> LOCAL_INFRASTRUCTURE =
            Set.of("postgres-db", "redis", "kafka");
    private static final Set<String> DEVELOPMENT_HELPERS =
            Set.of("migration-runner", "runtime-grants");
    private static final Set<String> ALLOWED_HOST_PORT_SERVICES =
            Set.of("gateway", "frontend", "postgres-db", "redis", "kafka");
    private static final Pattern PRIVATE_IPV4 = Pattern.compile(
            "(?m)(?:^|[^0-9])(?:10\\.|192\\.168\\.|172\\.(?:1[6-9]|2[0-9]|3[01])\\.)");

    @Test
    void developmentComposeBuildsEveryEnabledImageTargetExactlyOnce() throws IOException {
        List<ImageTarget> targets = enabledImageTargets();
        Map<String, Object> services = services(resolve("docker-compose.yml"));

        assertThat(targets).hasSize(36);
        assertThat(targets).filteredOn(target -> target.kind().equals("java-api")).hasSize(17);
        assertThat(targets).filteredOn(target -> target.kind().equals("java-batch")).hasSize(15);
        assertThat(targets)
                .filteredOn(target -> PLATFORM_SERVICES.contains(target.name()))
                .hasSize(4);

        Set<String> expectedExecutables = new LinkedHashSet<>();
        for (ImageTarget target : targets) {
            expectedExecutables.add(target.name());
            assertThat(services).as(target.name()).containsKey(target.name());
            Map<String, Object> service = asMap(services.get(target.name()));
            Map<String, Object> build = asMap(service.get("build"));

            if (target.kind().startsWith("java-")) {
                assertThat(build).as(target.name())
                        .containsEntry("context", ".")
                        .containsEntry("dockerfile", "Containerfile");
                assertThat(asMap(build.get("args"))).as(target.name())
                        .containsExactlyInAnyOrderEntriesOf(Map.of(
                                "GRADLE_PROJECT", target.gradleProject(),
                                "JAR_DIRECTORY", target.jarDirectory()));
            } else {
                assertThat(target.kind()).isEqualTo("frontend");
                assertThat(build).as(target.name())
                        .containsEntry("context", "./frontend")
                        .containsEntry("dockerfile", "Containerfile.dev");
            }
        }

        assertThat(services.keySet())
                .containsAll(expectedExecutables)
                .containsAll(LOCAL_INFRASTRUCTURE);
        assertThat(services.keySet())
                .filteredOn(name -> !LOCAL_INFRASTRUCTURE.contains(name))
                .containsExactlyInAnyOrderElementsOf(union(expectedExecutables, DEVELOPMENT_HELPERS));
    }

    @Test
    void selfContainedInfrastructureIsExplicitAndOnlyIntendedPortsArePublished()
            throws IOException {
        Map<String, Object> services = services(resolve("docker-compose.yml"));
        Map<String, Object> selfContained = services(resolve("compose.self-contained.yml"));

        for (String infrastructure : LOCAL_INFRASTRUCTURE) {
            Map<String, Object> service = asMap(services.get(infrastructure));
            assertThat(asList(service.get("profiles"))).as(infrastructure)
                    .isNotEmpty()
                    .allSatisfy(profile -> assertThat(profile.toString())
                            .contains("self-contained"));
            assertThat(service).as(infrastructure).containsKey("healthcheck");
        }

        Map<String, Object> selfContainedPostgresEnvironment = asMap(
                asMap(selfContained.get("postgres-db")).get("environment"));
        assertRequiredVariable(
                selfContainedPostgresEnvironment.get("POSTGRES_PASSWORD"),
                "postgres-db",
                "POSTGRES_ADMIN_PASSWORD");
        assertRequiredVariable(
                selfContainedPostgresEnvironment.get("ACCOUNT_DB_OWNER_PASSWORD"),
                "postgres-db",
                "ACCOUNT_DB_OWNER_PASSWORD");
        assertRequiredVariable(
                selfContainedPostgresEnvironment.get("ACCOUNT_DB_APP_PASSWORD"),
                "postgres-db",
                "ACCOUNT_DB_APP_PASSWORD");

        assertThat(asList(asMap(services.get("gateway")).get("ports")))
                .containsExactly("127.0.0.1:${DEV_GATEWAY_PORT:-8000}:8000");
        assertThat(asList(asMap(services.get("frontend")).get("ports")))
                .containsExactly("127.0.0.1:${DEV_FRONTEND_PORT:-3000}:3000");
        assertThat(asList(asMap(asMap(services.get("frontend")).get("healthcheck")).get("test")))
                .contains("wget -q -O /dev/null http://127.0.0.1:3000/ || exit 1");
        assertThat(asList(asMap(services.get("postgres-db")).get("ports")))
                .containsExactly("127.0.0.1:${POSTGRES_PORT:-5432}:5432");
        assertThat(asList(asMap(services.get("redis")).get("ports")))
                .containsExactly("127.0.0.1:${REDIS_PORT:-6379}:6379");
        assertThat(asList(asMap(services.get("kafka")).get("ports")))
                .containsExactly("127.0.0.1:${KAFKA_PORT:-9092}:9092");
        Map<String, Object> gatewayEnvironment =
                asMap(asMap(services.get("gateway")).get("environment"));
        assertThat(gatewayEnvironment)
                .containsEntry("MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED", "true");
        assertRequiredVariable(
                gatewayEnvironment.get("AUTH_JWT_SECRET"),
                "gateway",
                "AUTH_JWT_SECRET");
        Map<String, Object> authEnvironment =
                asMap(asMap(services.get("auth-api")).get("environment"));
        assertRequiredVariable(
                authEnvironment.get("AUTH_JWT_SECRET"), "auth-api", "AUTH_JWT_SECRET");
        assertRequiredVariable(
                authEnvironment.get("AUTH_DEFAULT_PASSWORD"),
                "auth-api",
                "AUTH_DEFAULT_PASSWORD");
        assertRequiredVariable(
                authEnvironment.get("AUTH_INTERNAL_API_TOKEN"),
                "auth-api",
                "AUTH_INTERNAL_API_TOKEN");
        assertThat(services.entrySet())
                .filteredOn(entry -> !ALLOWED_HOST_PORT_SERVICES.contains(entry.getKey()))
                .allSatisfy(entry -> assertThat(asMap(entry.getValue()))
                        .as(entry.getKey())
                        .doesNotContainKey("ports"));

        String compose = Files.readString(resolve("docker-compose.yml"));
        assertThat(compose)
                .doesNotContain(
                        "jdbc:h2:",
                        "dev_pass",
                        "POSTGRES_PASSWORD: postgres",
                        "ddl-auto: update",
                        "ddl-auto: create")
                .doesNotMatch(PRIVATE_IPV4);
    }

    @Test
    void domainServicesUsePostgresqlWithoutOwningRuntimeSchemaChangesAndBatchIsOptIn()
            throws IOException {
        Map<String, Object> services = services(resolve("docker-compose.yml"));
        Map<String, Object> selfContained = services(resolve("compose.self-contained.yml"));

        for (ImageTarget target : enabledImageTargets()) {
            if (!Set.of("java-api", "java-batch").contains(target.kind())) {
                continue;
            }
            String name = target.name();
            Map<String, Object> service = asMap(services.get(name));
            Map<String, Object> environment = asMap(service.get("environment"));
            Map<String, Object> postgresDependency =
                    asMap(asMap(asMap(selfContained.get(name)).get("depends_on"))
                            .get("self-contained-schema-check"));

            assertThat(environment).as(name)
                    .containsEntry("SPRING_PROFILES_ACTIVE", "dev")
                    .containsEntry("SPRING_DATASOURCE_DRIVER_CLASS_NAME", "org.postgresql.Driver")
                    .containsEntry("SPRING_FLYWAY_ENABLED", "false")
                    .containsEntry("SPRING_FLYWAY_CLEAN_DISABLED", "true")
                    .containsEntry("SPRING_JPA_HIBERNATE_DDL_AUTO", "validate")
                    .containsEntry("SPRING_SQL_INIT_MODE", "never")
                    .containsEntry(
                            "SPRING_KAFKA_BOOTSTRAP_SERVERS",
                            "${DEV_KAFKA_BOOTSTRAP_SERVERS:-kafka:9092}")
                    .containsEntry(
                            "SPRING_DATA_REDIS_HOST", "${DEV_REDIS_HOST:-redis}")
                    .containsEntry(
                            "SPRING_DATA_REDIS_PORT", "${DEV_REDIS_PORT:-6379}")
                    .containsEntry(
                            "EUREKA_CLIENT_SERVICEURL_DEFAULTZONE",
                            "http://discovery:8761/eureka/")
                    .doesNotContainKey("EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE");
            assertRequiredVariable(environment.get("SPRING_DATASOURCE_URL"), name, "_DB_URL");
            assertRequiredVariable(environment.get("SPRING_DATASOURCE_USERNAME"), name, "_DB_USER");
            assertRequiredVariable(environment.get("SPRING_DATASOURCE_PASSWORD"), name, "_DB_PASSWORD");
            assertThat(postgresDependency).as(name)
                    .containsEntry("condition", "service_healthy");

            if (target.kind().equals("java-batch")) {
                assertThat(asList(service.get("profiles"))).as(name).containsExactly("batch");
                assertThat(environment).as(name)
                        .containsEntry("SPRING_MAIN_WEB_APPLICATION_TYPE", "none")
                        .containsEntry("SPRING_BATCH_JOB_ENABLED", "false")
                        .containsEntry("SPRING_BATCH_JDBC_INITIALIZE_SCHEMA", "never");
                assertThat(service.get("restart")).as(name).isEqualTo("no");
            }
        }
    }

    @Test
    void externalDevelopmentOverlayUsesAuthenticatedProbeAsEveryDomainGate()
            throws IOException {
        Path composePath = resolve("compose.external-dev.yml");
        Map<String, Object> services = services(composePath);
        Map<String, Object> probe = asMap(services.get("external-dev-db-check"));
        Map<String, Object> environment = asMap(probe.get("environment"));
        String healthcheck = asList(asMap(probe.get("healthcheck")).get("test")).toString();
        String probeScript = Files.readString(resolve(
                "postgres", "runtime", "check-external-databases.sh"));

        assertThat(services).doesNotContainKeys("postgres-db", "redis", "kafka");
        assertThat(probe).doesNotContainKeys("ports", "build");
        assertThat(asList(probe.get("profiles"))).contains("external-dev");
        assertThat(healthcheck)
                .contains("sh", "/account-runtime/check-external-databases.sh");
        assertThat(probeScript)
                .contains(
                        "SELECT CASE WHEN",
                        "has_table_privilege(current_user",
                        "has_sequence_privilege(current_user",
                        "JOIN pg_namespace namespace",
                        "--dbname \"$connection_uri\"")
                .contains("2>/dev/null")
                .doesNotContain("echo \"$jdbc_url\"", "echo \"$password\"");
        assertThat(probeScript.lines())
                .filteredOn(line -> line.startsWith("check_database "))
                .hasSize(17);

        for (ImageTarget target : enabledImageTargets()) {
            if (!Set.of("java-api", "java-batch").contains(target.kind())) {
                continue;
            }
            Map<String, Object> service = asMap(services.get(target.name()));
            Map<String, Object> dependency =
                    asMap(asMap(service.get("depends_on")).get("external-dev-db-check"));
            assertThat(dependency).as(target.name())
                    .containsEntry("condition", "service_healthy");
            assertThat(service).as(target.name()).doesNotContainKey("ports");
        }
    }

    @Test
    void developmentEnvironmentExamplesContainOnlyDummyValues() throws IOException {
        for (String file : List.of(".env.dev.example", ".env.external-dev.example")) {
            String text = Files.readString(resolve(file));
            Map<String, String> values = loadEnvironmentExample(resolve(file));

            assertThat(text).as(file)
                    .doesNotMatch(PRIVATE_IPV4)
                    .doesNotContain("dev_pass", "password=postgres", "password=admin");
            assertThat(values).as(file).isNotEmpty();
            values.forEach((key, value) -> {
                if (key.contains("PASSWORD") || key.endsWith("_SECRET")) {
                    assertThat(value).as(file + " " + key)
                            .startsWith("replace-with-");
                }
            });
        }

        assertThat(loadEnvironmentExample(resolve(".env.external-dev.example")))
                .containsEntry(
                        "AUTH_DB_URL", "jdbc:postgresql://db.example.invalid:5432/auth_dev")
                .containsEntry("AUTH_DB_PASSWORD", "replace-with-secret-provider-value");
    }

    @Test
    void developmentEnvironmentValidatorSelfTestDoesNotEchoFixtureSecrets() throws Exception {
        String windowsDirectory = System.getenv("WINDIR");
        Assumptions.assumeTrue(windowsDirectory != null, "Windows PowerShell is not available");
        Path powerShell = Path.of(
                windowsDirectory,
                "System32",
                "WindowsPowerShell",
                "v1.0",
                "powershell.exe");
        Assumptions.assumeTrue(Files.isRegularFile(powerShell), "Windows PowerShell is not available");

        Process process = new ProcessBuilder(
                        powerShell.toString(),
                        "-NoProfile",
                        "-ExecutionPolicy",
                        "Bypass",
                        "-File",
                        resolve("validate-dev-env.ps1").toAbsolutePath().toString(),
                        "-SelfTest")
                .redirectErrorStream(true)
                .start();
        boolean finished = process.waitFor(15, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(finished).isTrue();
        assertThat(process.exitValue()).isZero();
        assertThat(output)
                .contains("Development environment validator self-test PASS")
                .doesNotContain(
                        "admin-secret-value",
                        "owner-secret-value",
                        "runtime-secret-value",
                        "local-auth-password");
    }

    private void assertRequiredVariable(Object value, String service, String suffix) {
        assertThat(value).as(service + " " + suffix).isNotNull();
        assertThat(value.toString()).as(service + " " + suffix)
                .startsWith("${")
                .contains(suffix)
                .contains(":?")
                .endsWith("}");
    }

    private List<ImageTarget> enabledImageTargets() throws IOException {
        JsonNode targets = new ObjectMapper()
                .readTree(resolve("deploy", "image-targets.json").toFile())
                .path("targets");
        List<ImageTarget> enabled = new ArrayList<>();
        for (JsonNode target : targets) {
            if (!target.path("enabled").asBoolean()) {
                continue;
            }
            enabled.add(new ImageTarget(
                    target.path("name").asText(),
                    target.path("kind").asText(),
                    target.path("gradleProject").asText(null),
                    target.path("jarDirectory").asText(null)));
        }
        return enabled;
    }

    private Map<String, Object> services(Path composePath) throws IOException {
        return asMap(loadYaml(composePath).get("services"));
    }

    private Map<String, Object> loadYaml(Path path) throws IOException {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setMaxAliasesForCollections(500);
        try (InputStream inputStream = Files.newInputStream(path)) {
            return asMap(new Yaml(new SafeConstructor(options)).load(inputStream));
        }
    }

    private Map<String, String> loadEnvironmentExample(Path path) throws IOException {
        Map<String, String> values = new LinkedHashMap<>();
        for (String line : Files.readAllLines(path)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String[] parts = trimmed.split("=", 2);
            assertThat(parts).as(path + " line: " + line).hasSize(2);
            values.put(parts[0], parts[1]);
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("expected YAML map but got: " + value);
        }
        return (Map<String, Object>) map;
    }

    private List<Object> asList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        return List.of(value);
    }

    private Path resolve(String... parts) {
        Path root = Files.exists(Path.of("settings.gradle")) ? Path.of("") : Path.of("..");
        Path resolved = root;
        for (String part : parts) {
            resolved = resolved.resolve(part);
        }
        return resolved;
    }

    private Set<String> union(Set<String> first, Set<String> second) {
        Set<String> result = new LinkedHashSet<>(first);
        result.addAll(second);
        return result;
    }

    private record ImageTarget(
            String name,
            String kind,
            String gradleProject,
            String jarDirectory) {}
}
