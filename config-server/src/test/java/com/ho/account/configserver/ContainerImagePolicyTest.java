package com.ho.account.configserver;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class ContainerImagePolicyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void manifestOwnsEveryExecutableTargetAndExcludesLibraries() throws IOException {
        JsonNode manifest = objectMapper.readTree(Files.readString(resolve("deploy", "image-targets.json")));
        JsonNode targets = manifest.get("targets");
        Set<String> names = new HashSet<>();
        Set<String> gradleProjects = new HashSet<>();
        Set<String> enabledGradleProjects = new HashSet<>();
        int javaTargets = 0;
        int enabledJavaTargets = 0;
        int frontendTargets = 0;

        for (JsonNode target : targets) {
            String name = target.get("name").asText();
            String kind = target.get("kind").asText();
            assertThat(names.add(name)).as("unique image name %s", name).isTrue();

            if ("frontend".equals(kind)) {
                frontendTargets++;
                assertThat(target.get("containerfile").asText()).isEqualTo("frontend/Containerfile");
                assertThat(target.get("context").asText()).isEqualTo("frontend");
                continue;
            }

            javaTargets++;
            String project = target.get("gradleProject").asText();
            String jarDirectory = target.get("jarDirectory").asText();
            assertThat(gradleProjects.add(project)).as("unique Gradle project %s", project).isTrue();
            assertThat(project).isEqualTo(":" + jarDirectory.replace("/", ":"));
            assertThat(resolve(jarDirectory, "build.gradle")).exists();
            assertThat(containsSpringBootApplication(resolve(jarDirectory, "src", "main", "java")))
                    .as("Spring Boot entry point for %s", project)
                    .isTrue();

            if (target.get("enabled").asBoolean()) {
                enabledJavaTargets++;
                enabledGradleProjects.add(project);
            } else {
                assertThat(name).isIn("internal-audit-api", "internal-audit-batch");
                assertThat(target.get("blockedBy").asText()).contains("#231");
            }
        }

        assertThat(targets.size()).isEqualTo(38);
        assertThat(javaTargets).isEqualTo(37);
        assertThat(enabledJavaTargets).isEqualTo(35);
        assertThat(frontendTargets).isEqualTo(1);
        assertThat(enabledGradleProjects)
                .containsExactlyInAnyOrderElementsOf(discoverEnabledExecutableProjects());
        assertThat(gradleProjects)
                .doesNotContain(":app", ":contracts", ":shared-kernel")
                .allMatch(project -> !project.endsWith(":core"));
    }

    @Test
    void canonicalJavaContainerfilesUseJava17AndDeterministicArtifactSelection()
            throws IOException {
        String containerfile = Files.readString(resolve("Containerfile"));
        String dockerfile = Files.readString(resolve("Dockerfile"));
        String dockerignore = Files.readString(resolve(".dockerignore"));

        assertThat(dockerfile).isEqualTo(containerfile);
        assertThat(containerfile)
                .contains("gradle:8.7-jdk17-alpine")
                .contains("eclipse-temurin:17-jre-alpine")
                .contains("ARG GRADLE_PROJECT")
                .contains("ARG JAR_DIRECTORY")
                .contains("chown gradle:gradle /workspace")
                .contains("USER gradle")
                .contains("./gradlew \"${GRADLE_PROJECT}:bootJar\"")
                .contains("test \"$jar_count\" -eq 1")
                .contains("! -name '*-plain.jar'")
                .contains("USER app:app")
                .contains("ENTRYPOINT [\"java\", \"-jar\", \"/app/app.jar\"]")
                .doesNotContain("jdk21")
                .doesNotContain("jre21")
                .doesNotContain("COPY --from=builder /workspace/*")
                .doesNotContain("ENTRYPOINT [\"sh\"");
        assertThat(dockerignore)
                .contains(
                        "**/.env",
                        "**/.env.*",
                        "**/application-local.properties",
                        "**/application-local.yml",
                        "**/application-local.yaml",
                        "**/secrets/**",
                        "**/*.key",
                        "**/*.pem",
                        "**/*.p12",
                        "**/*.jks",
                        "**/build")
                .doesNotContain("!frontend/.env");
    }

    @Test
    void frontendUsesStandaloneCanonicalContainerfile() throws IOException {
        String containerfile = Files.readString(resolve("frontend", "Containerfile"));
        String nextConfig = Files.readString(resolve("frontend", "next.config.ts"));
        String compose = Files.readString(resolve("frontend", "docker-compose.yml"));
        String dockerignore = Files.readString(resolve("frontend", ".dockerignore"));

        assertThat(nextConfig).contains("output: \"standalone\"");
        assertThat(containerfile)
                .contains("FROM node:20-alpine")
                .contains("RUN npm ci")
                .contains("/app/.next/standalone")
                .contains("USER nextjs")
                .contains("CMD [\"node\", \"server.js\"]");
        assertThat(compose)
                .contains("dockerfile: Containerfile")
                .contains("NEXT_PUBLIC_API_URL:-/api")
                .contains("NODE_ENV=production")
                .doesNotContain("volumes:")
                .doesNotContain("NODE_ENV=development");
        assertThat(dockerignore).contains(
                "node_modules",
                ".next",
                ".env",
                "application-local.yml",
                "**/secrets/**",
                "**/*.key",
                "**/*.pem",
                "**/*.p12",
                "**/*.jks");
    }

    @Test
    void moduleComposeFilesSelectTheCanonicalContainerfileAndExactGradleProject()
            throws IOException {
        Map<String, ComposeTarget> targets = new LinkedHashMap<>();
        targets.put("auth/docker-compose.yml",
                new ComposeTarget("auth", "..", ":auth:api", "auth/api"));
        targets.put("budget/docker-compose.yml",
                new ComposeTarget("budget-api", "..", ":budget:api", "budget/api"));
        targets.put("master-data/docker-compose.yml",
                new ComposeTarget("master-data", "..", ":master-data:api", "master-data/api"));
        targets.put("asset-lease/docker-compose.yml",
                new ComposeTarget("asset-lease", "..", ":asset-lease:api", "asset-lease/api"));
        targets.put("closing/docker-compose.yml",
                new ComposeTarget("closing", "..", ":closing:api", "closing/api"));
        targets.put("journal-ledger/docker-compose.yml",
                new ComposeTarget("journal-ledger", "..", ":journal-ledger:api", "journal-ledger/api"));
        targets.put("loan/docker-compose.yml",
                new ComposeTarget("loan", "..", ":loan:api", "loan/api"));
        targets.put("payable/docker-compose.yml",
                new ComposeTarget("payable", "..", ":payable:api", "payable/api"));
        targets.put("receivable/docker-compose.yml",
                new ComposeTarget("receivable", "..", ":receivable:api", "receivable/api"));
        targets.put("reconciliation/docker-compose.yml",
                new ComposeTarget("reconciliation", "..", ":reconciliation:api", "reconciliation/api"));
        targets.put("reporting/docker-compose.yml",
                new ComposeTarget("reporting", "..", ":reporting:api", "reporting/api"));
        targets.put("tax/docker-compose.yml",
                new ComposeTarget("tax", "..", ":tax:api", "tax/api"));
        targets.put("expenditure-resolution/docker-compose.yml",
                new ComposeTarget("expenditure-resolution", "..", ":expenditure-resolution:api",
                        "expenditure-resolution/api"));
        targets.put("account-mart/mart-api/docker-compose.yml",
                new ComposeTarget("ifrs9-allowance-mart-api", "../..", ":account-mart:mart-api",
                        "account-mart/mart-api"));
        targets.put("account-mart/mart-batch/docker-compose.yml",
                new ComposeTarget("ifrs9-allowance-mart-batch", "../..", ":account-mart:mart-batch",
                        "account-mart/mart-batch"));
        targets.put("ecl/ecl-api/docker-compose.yml",
                new ComposeTarget("allowance-ecl-api", "../..", ":ecl:ecl-api", "ecl/ecl-api"));
        targets.put("ecl/ecl-batch/docker-compose.yml",
                new ComposeTarget("allowance-ecl-batch", "../..", ":ecl:ecl-batch", "ecl/ecl-batch"));
        targets.put("config-server/docker-compose.yml",
                new ComposeTarget("config-server", "..", ":config-server", "config-server"));
        targets.put("discovery/docker-compose.yml",
                new ComposeTarget("discovery", "..", ":discovery", "discovery"));
        targets.put("gateway/docker-compose.yml",
                new ComposeTarget("gateway", "..", ":gateway", "gateway"));

        assertThat(targets).hasSize(20);
        for (Map.Entry<String, ComposeTarget> entry : targets.entrySet()) {
            Map<String, Object> root = loadYaml(resolve(entry.getKey().split("/")));
            ComposeTarget target = entry.getValue();
            Map<String, Object> service = asMap(asMap(root.get("services")).get(target.service()));
            Map<String, Object> build = asMap(service.get("build"));
            Map<String, Object> args = asMap(build.get("args"));

            assertThat(build.get("context")).as(entry.getKey()).isEqualTo(target.context());
            assertThat(build.get("dockerfile")).as(entry.getKey()).isEqualTo("Containerfile");
            assertThat(args).as(entry.getKey())
                    .containsEntry("GRADLE_PROJECT", target.gradleProject())
                    .containsEntry("JAR_DIRECTORY", target.jarDirectory());
        }
    }

    @Test
    void activeModuleComposeFilesRequireDatabaseCredentialsAndNeverUpdateSchema()
            throws IOException {
        Map<String, String> databaseVariables = Map.of(
                "auth/docker-compose.yml", "AUTH_DB",
                "account-mart/mart-api/docker-compose.yml", "ACCOUNT_MART_DB",
                "account-mart/mart-batch/docker-compose.yml", "ACCOUNT_MART_DB",
                "ecl/ecl-api/docker-compose.yml", "ECL_DB",
                "ecl/ecl-batch/docker-compose.yml", "ECL_DB");

        for (Map.Entry<String, String> entry : databaseVariables.entrySet()) {
            String compose = Files.readString(resolve(entry.getKey().split("/")));
            String variablePrefix = entry.getValue();
            assertThat(compose).as(entry.getKey())
                    .contains(
                            "${" + variablePrefix + "_URL:?set " + variablePrefix + "_URL}",
                            "${" + variablePrefix + "_USER:?set " + variablePrefix + "_USER}",
                            "${" + variablePrefix + "_PASSWORD:?set "
                                    + variablePrefix + "_PASSWORD}")
                    .doesNotContain(
                            "SPRING_DATASOURCE_PASSWORD=postgres",
                            "allowance_password",
                            "DDL_AUTO=update");
        }
    }

    @Test
    void verificationToolDrainsBothProcessStreamsConcurrently() throws IOException {
        String script = Files.readString(resolve("tools", "container-images.ps1"));

        assertThat(script)
                .contains(
                        "StandardOutput.ReadToEndAsync()",
                        "StandardError.ReadToEndAsync()",
                        "$standardOutputTask.Result",
                        "$standardErrorTask.Result")
                .doesNotContain("StandardOutput.ReadToEnd()");
    }

    private Map<String, Object> loadYaml(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return asMap(new Yaml().load(inputStream));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("expected YAML map but got: " + value);
        }
        return (Map<String, Object>) map;
    }

    private boolean containsSpringBootApplication(Path sourceRoot) throws IOException {
        if (!Files.isDirectory(sourceRoot)) {
            return false;
        }
        try (var paths = Files.walk(sourceRoot)) {
            return paths.filter(path -> path.toString().endsWith(".java"))
                    .anyMatch(path -> {
                        try {
                            return Files.readString(path).contains("@SpringBootApplication");
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    });
        }
    }

    private Set<String> discoverEnabledExecutableProjects() throws IOException {
        Path repositoryRoot = Files.exists(Path.of("settings.gradle"))
                ? Path.of("").toAbsolutePath().normalize()
                : Path.of("..").toAbsolutePath().normalize();
        Set<String> projects = new HashSet<>();
        try (var paths = Files.walk(repositoryRoot)) {
            for (Path buildFile : paths
                    .filter(path -> path.getFileName().toString().equals("build.gradle"))
                    .toList()) {
                String build = Files.readString(buildFile);
                Path projectRoot = buildFile.getParent();
                if (!(build.contains("id 'org.springframework.boot'")
                                || inheritsSpringBootPlugin(projectRoot, repositoryRoot))
                        || build.matches("(?s).*bootJar\\s*\\{[^}]*enabled\\s*=\\s*false.*")
                        || !containsSpringBootApplication(projectRoot.resolve("src/main/java"))) {
                    continue;
                }
                String project = ":" + repositoryRoot.relativize(projectRoot)
                        .toString()
                        .replace('\\', ':')
                        .replace('/', ':');
                String leaf = project.substring(project.lastIndexOf(':') + 1);
                if (!leaf.endsWith("api")
                        && !leaf.endsWith("batch")
                        && !Set.of(":config-server", ":discovery", ":gateway").contains(project)) {
                    continue;
                }
                projects.add(project);
            }
        }
        return projects;
    }

    private boolean inheritsSpringBootPlugin(Path projectRoot, Path repositoryRoot)
            throws IOException {
        for (Path ancestor = projectRoot.getParent();
                ancestor != null && !ancestor.equals(repositoryRoot);
                ancestor = ancestor.getParent()) {
            Path buildFile = ancestor.resolve("build.gradle");
            if (Files.exists(buildFile)
                    && Files.readString(buildFile)
                            .contains("apply plugin: 'org.springframework.boot'")) {
                return true;
            }
        }
        return false;
    }

    private Path resolve(String... parts) {
        Path root = Files.exists(Path.of("settings.gradle")) ? Path.of("") : Path.of("..");
        Path resolved = root;
        for (String part : parts) {
            resolved = resolved.resolve(part);
        }
        return resolved;
    }

    private record ComposeTarget(
            String service,
            String context,
            String gradleProject,
            String jarDirectory) {
    }
}
