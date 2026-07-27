package com.ho.account.masterdata;

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

import static org.assertj.core.api.Assertions.assertThat;

class MasterDataConfigurationPolicyTest {

    @Test
    void localDefaultsUsePort8082AndDatabaseReadiness() {
        Properties properties = loadProperties(resolveFromRepositoryRoot(
                "master-data", "src", "main", "resources", "application.yml"));

        assertThat(properties.getProperty("server.port")).isEqualTo("${SERVER_PORT:8082}");
        assertThat(properties.getProperty("spring.config.import"))
                .isEqualTo("optional:configserver:http://localhost:8888/");
        assertThat(properties.getProperty("management.endpoint.health.probes.enabled")).isEqualTo("true");
        assertThat(properties.getProperty("management.endpoint.health.group.readiness.include"))
                .isEqualTo("readinessState,db");
    }

    @Test
    void rootAndModuleComposeUsePort8082AndRepositoryRootBuildContext() throws IOException {
        Map<String, Object> root = loadYaml(resolveFromRepositoryRoot("docker-compose.yml"));
        Map<String, Object> rootService = asMap(asMap(root.get("services")).get("master-data"));
        assertThat(asList(rootService.get("ports"))).containsExactly("8082:8082");
        assertThat(asList(rootService.get("environment")))
                .contains(
                        "SERVER_PORT=8082",
                        "SPRING_CONFIG_IMPORT=optional:configserver:http://config-server:8888/",
                        "EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery:8761/eureka/")
                .noneMatch(value -> value.toString().startsWith("SPRING_KAFKA_"))
                .noneMatch(value -> value.toString().startsWith("SPRING_REDIS_"));

        Map<String, Object> module = loadYaml(resolveFromRepositoryRoot("master-data", "docker-compose.yml"));
        Map<String, Object> moduleService = asMap(asMap(module.get("services")).get("master-data"));
        Map<String, Object> build = asMap(moduleService.get("build"));
        assertThat(build.get("context")).isEqualTo("..");
        assertThat(build.get("dockerfile")).isEqualTo("master-data/Dockerfile");
        assertThat(asList(moduleService.get("ports"))).containsExactly("8082:8082");
    }

    @Test
    void dockerAndIntellijUseJava17SingleBootJarAndReadiness() throws IOException {
        String dockerfile = Files.readString(resolveFromRepositoryRoot("master-data", "Dockerfile"));
        assertThat(dockerfile)
                .contains("gradle:8.7-jdk17-alpine")
                .contains("eclipse-temurin:17-jre-alpine")
                .contains(":master-data:bootJar")
                .contains("http://localhost:8082/actuator/health/readiness")
                .doesNotContain("jdk21")
                .doesNotContain("*-SNAPSHOT.jar")
                .doesNotContain("|| true");

        String runConfiguration = Files.readString(resolveFromRepositoryRoot(
                ".run", "Master Data bootRun.run.xml"));
        assertThat(runConfiguration)
                .contains(":master-data:bootRun")
                .contains("--server.port=8082")
                .contains("--spring.jpa.hibernate.ddl-auto=create-drop")
                .contains("--spring.flyway.enabled=false")
                .contains("--no-daemon");
    }

    @Test
    void migrationsPreserveHistoricalV2AndUseForwardSchemaCorrections() throws IOException {
        String createRequest = Files.readString(resolveFromRepositoryRoot(
                "master-data", "src", "main", "resources", "db", "migration",
                "V2__master_data_change_requests.sql"));
        String addLockVersion = Files.readString(resolveFromRepositoryRoot(
                "master-data", "src", "main", "resources", "db", "migration",
                "V3__master_data_change_request_lock_version.sql"));
        String changePayloadType = Files.readString(resolveFromRepositoryRoot(
                "master-data", "src", "main", "resources", "db", "migration",
                "V4__master_data_change_request_payload_text.sql"));
        String addLineage = Files.readString(resolveFromRepositoryRoot(
                "master-data", "src", "main", "resources", "db", "migration",
                "V5__master_data_change_request_lineage.sql"));

        assertThat(createRequest).contains("payload_json CLOB");
        assertThat(addLockVersion).contains("lock_version BIGINT NOT NULL DEFAULT 0");
        assertThat(changePayloadType)
                .contains("ALTER COLUMN payload_json SET DATA TYPE TEXT")
                .doesNotContain("CLOB");
        assertThat(addLineage)
                .contains("source_reference VARCHAR(120)")
                .contains("applied_at TIMESTAMP")
                .contains("uq_mdc_source_reference");
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
