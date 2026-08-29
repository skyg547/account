package com.ho.account.configserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

class LogstashDevelopmentPolicyTest {

    @Test
    void developmentLogstashUsesMountedJsonPipelineAndFailClosedHealth() throws IOException {
        Map<String, Object> compose = loadYaml(resolve("logstash", "docker-compose.yml"));
        Map<String, Object> services = asMap(compose.get("services"));
        Map<String, Object> logstash = asMap(services.get("logstash"));
        Map<String, Object> healthcheck = asMap(logstash.get("healthcheck"));
        Map<String, Object> limits = asMap(
                asMap(asMap(logstash.get("deploy")).get("resources")).get("limits"));
        Map<String, Object> network = asMap(
                asMap(compose.get("networks")).get("account-network"));
        List<Object> healthTest = asList(healthcheck.get("test"));
        String nodeConfig = Files.readString(resolve("logstash", "config", "logstash.yml"));
        String pipeline = Files.readString(resolve("logstash", "pipeline", "logstash.conf"));
        String readme = Files.readString(resolve("logstash", "README.md"));

        assertThat(compose).containsEntry("name", "account-logstash-dev");
        assertThat(services).containsOnlyKeys("logstash");
        assertThat(logstash)
                .containsEntry("image", "docker.elastic.co/logstash/logstash:7.17.10")
                .containsEntry("container_name", "account-logstash-dev")
                .containsEntry("stop_grace_period", "30s")
                .doesNotContainKey("ports");
        assertThat(asList(logstash.get("expose"))).containsExactly("5000", "9600");
        assertThat(asList(logstash.get("volumes")))
                .containsExactlyInAnyOrder(
                        "./config/logstash.yml:/usr/share/logstash/config/logstash.yml:ro",
                        "./pipeline/logstash.conf:/usr/share/logstash/pipeline/logstash.conf:ro");
        assertThat(healthTest).hasSize(2);
        assertThat(healthTest.get(0)).isEqualTo("CMD-SHELL");
        String healthCommand = healthTest.get(1).toString();
        assertThat(healthCommand)
                .contains(
                        "curl --fail",
                        "127.0.0.1:9600/_node/pipelines/main",
                        "&&",
                        "elasticsearch:9200/_cluster/health?wait_for_status=yellow&timeout=2s",
                        "ES_HEALTH=\"$$(curl",
                        "\"$${ES_HEALTH}\"",
                        "grep -Eq",
                        "\"timed_out\"[[:space:]]*:[[:space:]]*false",
                        "\"status\"[[:space:]]*:[[:space:]]*\"(yellow|green)\"")
                .doesNotContain(";", "|| true");
        assertThat(limits)
                .containsEntry("cpus", "0.50")
                .containsEntry("memory", "640m")
                .containsEntry("pids", 256);
        assertThat(network)
                .containsEntry("name", "${ACCOUNT_NETWORK_NAME:-account-network}")
                .containsEntry("external", true);

        assertThat(nodeConfig)
                .contains(
                        "http.host: \"0.0.0.0\"",
                        "http.port: 9600",
                        "pipeline.workers: 1",
                        "pipeline.ecs_compatibility: disabled",
                        "config.reload.automatic: false",
                        "xpack.monitoring.enabled: false");
        assertThat(pipeline)
                .contains(
                        "port => 5000",
                        "codec => json_lines",
                        "hosts => [\"http://elasticsearch:9200\"]",
                        "index => \"account-logs-dev-%{+YYYY.MM.dd}\"")
                .doesNotContain("5044", "stdout", "rubydebug");
        assertThat(readme)
                .contains(
                        "config --quiet",
                        "--config.test_and_exit",
                        "synthetic_count=1",
                        "stop logstash",
                        "exited")
                .doesNotContain("prune --all");
    }

    private Map<String, Object> loadYaml(Path path) throws IOException {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        try (InputStream inputStream = Files.newInputStream(path)) {
            return asMap(new Yaml(new SafeConstructor(options)).load(inputStream));
        }
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
}
