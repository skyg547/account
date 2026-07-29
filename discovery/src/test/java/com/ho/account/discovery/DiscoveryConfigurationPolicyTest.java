package com.ho.account.discovery;

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

class DiscoveryConfigurationPolicyTest {

    @Test
    void localDefaultsStartSingleNodeRegistryOnPort8761WithoutConfigServer() {
        Properties properties = loadProperties(resolveFromRepositoryRoot(
                "discovery", "src", "main", "resources", "application.yml"));

        assertThat(properties.getProperty("server.port")).isEqualTo("${SERVER_PORT:8761}");
        assertThat(properties.getProperty("spring.config.import"))
                .isEqualTo("optional:configserver:http://localhost:8888/");
        assertThat(properties.getProperty("eureka.client.register-with-eureka")).isEqualTo("false");
        assertThat(properties.getProperty("eureka.client.fetch-registry")).isEqualTo("false");
        assertThat(properties.getProperty("management.endpoint.health.probes.enabled")).isEqualTo("true");
    }

    @Test
    void externalConfigurationKeepsSamePortAndServerOnlyClientPolicy() {
        Properties properties = loadProperties(resolveFromRepositoryRoot(
                "config-repo", "discovery-service.yml"));

        assertThat(properties.getProperty("server.port")).isEqualTo("${SERVER_PORT:8761}");
        assertThat(properties.getProperty("eureka.client.register-with-eureka")).isEqualTo("false");
        assertThat(properties.getProperty("eureka.client.fetch-registry")).isEqualTo("false");
        assertThat(properties.getProperty("management.endpoints.web.exposure.include"))
                .isEqualTo("health,info,prometheus");
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
