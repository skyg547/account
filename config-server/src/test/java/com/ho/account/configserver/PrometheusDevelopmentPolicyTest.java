package com.ho.account.configserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

class PrometheusDevelopmentPolicyTest {

    @Test
    void externalDevelopmentPrometheusIsBoundedAndTargetsOnlyTheMinimalAuthStack()
            throws IOException {
        Map<String, Object> compose = loadYaml(resolve("prometheus", "docker-compose.yml"));
        Map<String, Object> services = asMap(compose.get("services"));
        Map<String, Object> prometheus = asMap(services.get("prometheus-dev"));
        Map<String, Object> healthcheck = asMap(prometheus.get("healthcheck"));
        Map<String, Object> limits = asMap(
                asMap(asMap(prometheus.get("deploy")).get("resources")).get("limits"));
        Map<String, Object> configuration =
                loadYaml(resolve("prometheus", "prometheus.yml"));
        String configurationText =
                Files.readString(resolve("prometheus", "prometheus.yml"));
        String readme = Files.readString(resolve("prometheus", "README.md"));

        assertThat(compose).containsEntry("name", "account-prometheus-dev");
        assertThat(services).containsOnlyKeys("prometheus-dev");
        assertThat(prometheus)
                .containsEntry("image", "docker.io/prom/prometheus:v3.14.0")
                .containsEntry("container_name", "account-prometheus-dev")
                .containsEntry("restart", "unless-stopped");
        assertThat(asList(prometheus.get("ports")))
                .containsExactly("127.0.0.1:${PROMETHEUS_DEV_PORT:-19090}:9090");
        assertThat(asList(prometheus.get("volumes")))
                .containsExactly(
                        "./prometheus.yml:/etc/prometheus/prometheus.yml:ro",
                        "prometheus_dev_data:/prometheus");
        assertThat(asList(prometheus.get("command")))
                .containsExactly(
                        "--config.file=/etc/prometheus/prometheus.yml",
                        "--storage.tsdb.path=/prometheus",
                        "--web.console.libraries=/usr/share/prometheus/console_libraries",
                        "--web.console.templates=/usr/share/prometheus/consoles")
                .doesNotContain("--web.enable-lifecycle");
        assertThat(asList(healthcheck.get("test")))
                .containsExactly(
                        "CMD-SHELL",
                        "wget -q -T 4 -t 1 -O /dev/null "
                                + "http://127.0.0.1:9090/-/ready || exit 1");
        assertThat(limits)
                .containsEntry("cpus", "0.50")
                .containsEntry("memory", "512M")
                .containsEntry("pids", 128);
        assertThat(asList(prometheus.get("networks"))).containsExactly("account-network");
        assertThat(asMap(compose.get("volumes"))).containsOnlyKeys("prometheus_dev_data");
        assertThat(asMap(asMap(compose.get("networks")).get("account-network")))
                .containsEntry("external", true);

        assertThat(targetsByJob(configuration))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "prometheus", List.of("localhost:9090"),
                        "minimal-auth", List.of("minimal-auth:8084"),
                        "minimal-master-data", List.of("minimal-master-data:8082"),
                        "minimal-discovery", List.of("minimal-discovery:8761"),
                        "minimal-config-server", List.of("minimal-config-server:8888"),
                        "minimal-gateway", List.of("minimal-gateway:8000")));
        assertThat(configurationText)
                .doesNotContain("host.docker.internal");
        assertThat(readme)
                .contains(
                        "targets total=6 up=6 down=0",
                        "stop prometheus-dev",
                        "--force-recreate prometheus-dev",
                        "127.0.0.1:19090",
                        "account-prometheus-dev",
                        "`down -v`",
                        "`prune`")
                .doesNotContain("podman stop --all");
    }

    private Map<String, List<String>> targetsByJob(Map<String, Object> configuration) {
        Map<String, List<String>> targets = new LinkedHashMap<>();
        List<Object> scrapeConfigs = asList(configuration.get("scrape_configs"));
        assertThat(scrapeConfigs).hasSize(6);
        for (Object scrapeObject : scrapeConfigs) {
            Map<String, Object> scrape = asMap(scrapeObject);
            String job = scrape.get("job_name").toString();
            List<Object> staticConfigs = asList(scrape.get("static_configs"));
            assertThat(staticConfigs).as(job).hasSize(1);
            assertThat(targets).as("duplicate job_name: " + job).doesNotContainKey(job);
            targets.put(job, asStringList(asMap(staticConfigs.get(0)).get("targets")));
            if (!job.equals("prometheus")) {
                assertThat(scrape).as(job)
                        .containsEntry("metrics_path", "/actuator/prometheus");
            }
        }
        return targets;
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

    private List<String> asStringList(Object value) {
        return asList(value).stream().map(Object::toString).toList();
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
