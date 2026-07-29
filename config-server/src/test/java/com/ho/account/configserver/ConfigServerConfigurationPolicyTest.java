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
    void localDefaultsUseNativeRepositoryAndRepositoryAwareReadiness() {
        Properties properties = loadProperties(resolveFromRepositoryRoot(
                "config-server", "src", "main", "resources", "application.yml"));

        assertThat(properties.getProperty("server.port")).isEqualTo("${SERVER_PORT:8888}");
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
    void moduleComposeUsesRootBuildContextAndReadOnlyRepositoryMount() throws IOException {
        Map<String, Object> root = loadYaml(resolveFromRepositoryRoot("config-server", "docker-compose.yml"));
        Map<String, Object> configServer = asMap(asMap(root.get("services")).get("config-server"));
        Map<String, Object> build = asMap(configServer.get("build"));

        assertThat(build.get("context")).isEqualTo("..");
        assertThat(build.get("dockerfile")).isEqualTo("Containerfile");
        assertThat(asMap(build.get("args")))
                .containsEntry("GRADLE_PROJECT", ":config-server")
                .containsEntry("JAR_DIRECTORY", "config-server");
        assertThat(asList(configServer.get("volumes"))).contains("../config-repo:/config-repo:ro");
        assertThat(asList(configServer.get("environment")))
                .contains("SPRING_PROFILES_ACTIVE=native", "CONFIG_REPO_LOCATION=file:/config-repo");
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
