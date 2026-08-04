package com.ho.account.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class GatewayDockerConfigurationTest {



    @Test
    void moduleComposeBuildsFromRepositoryRoot() throws IOException {
        Map<String, Object> root = loadYaml(resolveFromRepositoryRoot("gateway", "docker-compose.yml"));
        Map<String, Object> gateway = service(root, "gateway");
        Map<String, Object> build = asMap(gateway.get("build"));

        assertThat(build.get("context")).isEqualTo("..");
        assertThat(build.get("dockerfile")).isEqualTo("Containerfile");
        assertThat(asMap(build.get("args")))
                .containsEntry("GRADLE_PROJECT", ":gateway")
                .containsEntry("JAR_DIRECTORY", "gateway");
        assertThat(asList(gateway.get("ports"))).contains("8000:8000");
        assertThat(asList(gateway.get("environment")))
                .contains("AUTH_TOKEN_VERSION_VALIDATION_BASE_URL=http://auth:8084");
    }

    private Map<String, Object> service(Map<String, Object> root, String serviceName) {
        return asMap(asMap(root.get("services")).get(serviceName));
    }

    private Map<String, Object> loadYaml(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            Object loaded = new Yaml().load(inputStream);
            return asMap(loaded);
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
