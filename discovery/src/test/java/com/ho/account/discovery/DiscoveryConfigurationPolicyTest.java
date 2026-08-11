package com.ho.account.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class DiscoveryConfigurationPolicyTest {

    @Test
    void localDefaultsStartSingleNodeRegistryOnPort8761WithoutConfigServer() throws IOException {
        Map<String, Object> doc = findDefaultDocument(resolveFromRepositoryRoot(
                "discovery", "src", "main", "resources", "application.yml"));

        assertThat(doc).isNotNull();

        Map<String, Object> server = asMap(doc.get("server"));
        assertThat(server.get("port")).isEqualTo("${SERVER_PORT:8761}");

        Map<String, Object> spring = asMap(doc.get("spring"));
        Map<String, Object> config = asMap(spring.get("config"));
        assertThat(config.get("import")).isEqualTo("optional:configserver:http://localhost:8888/");

        Map<String, Object> eureka = asMap(doc.get("eureka"));
        Map<String, Object> client = asMap(eureka.get("client"));
        assertThat(client.get("register-with-eureka")).isEqualTo(false);
        assertThat(client.get("fetch-registry")).isEqualTo(false);

        Map<String, Object> eurekaServer = asMap(eureka.get("server"));
        assertThat(eurekaServer.get("enable-self-preservation"))
                .isEqualTo("${EUREKA_SERVER_ENABLE_SELF_PRESERVATION:true}");
        assertThat(eurekaServer.get("eviction-interval-timer-in-ms"))
                .isEqualTo("${EUREKA_SERVER_EVICTION_INTERVAL_TIMER_IN_MS:60000}");

        Map<String, Object> management = asMap(doc.get("management"));
        Map<String, Object> endpoint = asMap(management.get("endpoint"));
        Map<String, Object> health = asMap(endpoint.get("health"));
        Map<String, Object> probes = asMap(health.get("probes"));
        assertThat(probes.get("enabled")).isEqualTo(true);
    }

    @Test
    void peer1ProfileConfiguresPeerAwarenessAndCrossReferenceToPeer2() throws IOException {
        Map<String, Object> peer1Doc = findDocumentByProfile(
                resolveFromRepositoryRoot("discovery", "src", "main", "resources", "application.yml"), "peer1");

        assertThat(peer1Doc).isNotNull();

        Map<String, Object> server = asMap(peer1Doc.get("server"));
        assertThat(server.get("port")).isEqualTo("${SERVER_PORT:8761}");

        Map<String, Object> eureka = asMap(peer1Doc.get("eureka"));
        Map<String, Object> instance = asMap(eureka.get("instance"));
        assertThat(instance.get("hostname")).isEqualTo("${EUREKA_INSTANCE_HOSTNAME:peer1}");

        Map<String, Object> client = asMap(eureka.get("client"));
        assertThat(client.get("register-with-eureka")).isEqualTo(true);
        assertThat(client.get("fetch-registry")).isEqualTo(true);

        Map<String, Object> serviceUrl = asMap(client.get("service-url"));
        assertThat(serviceUrl.get("defaultZone"))
                .isEqualTo("${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:http://peer2:8762/eureka/}");
    }

    @Test
    void peer2ProfileConfiguresPeerAwarenessAndCrossReferenceToPeer1() throws IOException {
        Map<String, Object> peer2Doc = findDocumentByProfile(
                resolveFromRepositoryRoot("discovery", "src", "main", "resources", "application.yml"), "peer2");

        assertThat(peer2Doc).isNotNull();

        Map<String, Object> server = asMap(peer2Doc.get("server"));
        assertThat(server.get("port")).isEqualTo("${SERVER_PORT:8762}");

        Map<String, Object> eureka = asMap(peer2Doc.get("eureka"));
        Map<String, Object> instance = asMap(eureka.get("instance"));
        assertThat(instance.get("hostname")).isEqualTo("${EUREKA_INSTANCE_HOSTNAME:peer2}");

        Map<String, Object> client = asMap(eureka.get("client"));
        assertThat(client.get("register-with-eureka")).isEqualTo(true);
        assertThat(client.get("fetch-registry")).isEqualTo(true);

        Map<String, Object> serviceUrl = asMap(client.get("service-url"));
        assertThat(serviceUrl.get("defaultZone"))
                .isEqualTo("${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:http://peer1:8761/eureka/}");
    }

    @Test
    void externalConfigurationKeepsSamePortAndServerOnlyClientPolicy() throws IOException {
        Map<String, Object> doc = findDefaultDocument(resolveFromRepositoryRoot(
                "config-repo", "discovery-service.yml"));

        assertThat(doc).isNotNull();

        Map<String, Object> server = asMap(doc.get("server"));
        assertThat(server.get("port")).isEqualTo("${SERVER_PORT:8761}");

        Map<String, Object> eureka = asMap(doc.get("eureka"));
        Map<String, Object> client = asMap(eureka.get("client"));
        assertThat(client.get("register-with-eureka")).isEqualTo(false);
        assertThat(client.get("fetch-registry")).isEqualTo(false);

        Map<String, Object> eurekaServer = asMap(eureka.get("server"));
        assertThat(eurekaServer.get("enable-self-preservation"))
                .isEqualTo("${EUREKA_SERVER_ENABLE_SELF_PRESERVATION:true}");
        assertThat(eurekaServer.get("eviction-interval-timer-in-ms"))
                .isEqualTo("${EUREKA_SERVER_EVICTION_INTERVAL_TIMER_IN_MS:60000}");

        Map<String, Object> management = asMap(doc.get("management"));
        Map<String, Object> endpoints = asMap(management.get("endpoints"));
        Map<String, Object> web = asMap(endpoints.get("web"));
        Map<String, Object> exposure = asMap(web.get("exposure"));
        assertThat(exposure.get("include")).isEqualTo("health,info,prometheus");
    }

    private Map<String, Object> findDefaultDocument(Path path) throws IOException {
        List<Map<String, Object>> documents = loadAllYamlDocuments(path);
        for (Map<String, Object> doc : documents) {
            Object springObj = doc.get("spring");
            if (springObj instanceof Map<?, ?> springMap) {
                Object configObj = springMap.get("config");
                if (configObj instanceof Map<?, ?> configMap) {
                    if (configMap.containsKey("activate")) {
                        continue;
                    }
                }
            }
            return doc;
        }
        return documents.isEmpty() ? null : documents.get(0);
    }

    private Map<String, Object> findDocumentByProfile(Path path, String profile) throws IOException {
        List<Map<String, Object>> documents = loadAllYamlDocuments(path);
        for (Map<String, Object> doc : documents) {
            Object springObj = doc.get("spring");
            if (springObj instanceof Map<?, ?> springMap) {
                Object configObj = springMap.get("config");
                if (configObj instanceof Map<?, ?> configMap) {
                    Object activateObj = configMap.get("activate");
                    if (activateObj instanceof Map<?, ?> activateMap) {
                        if (profile.equals(activateMap.get("on-profile"))) {
                            return doc;
                        }
                    }
                }
            }
        }
        return null;
    }

    private List<Map<String, Object>> loadAllYamlDocuments(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            List<Map<String, Object>> docs = new ArrayList<>();
            for (Object doc : new Yaml().loadAll(inputStream)) {
                if (doc != null) {
                    docs.add(asMap(doc));
                }
            }
            return docs;
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
}


