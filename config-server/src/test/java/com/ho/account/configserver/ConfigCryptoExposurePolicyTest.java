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
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

class ConfigCryptoExposurePolicyTest {
    private final Path root = Files.exists(Path.of("settings.gradle")) ? Path.of("") : Path.of("..");

    @Test
    void everySupportedComposeKeepsConfigServerOffPublicIngress() throws IOException {
        for (String file : List.of("docker-compose.yml", "config-server/docker-compose.yml",
                "compose.prod.yml", "tools/compose.minimal-auth-external-dev.yml")) {
            Map<?, ?> services = (Map<?, ?>) yaml(file).get("services");
            String name = file.startsWith("tools/") ? "minimal-config-server" : "config-server";
            Map<?, ?> service = (Map<?, ?>) services.get(name);
            assertThat(service).as(file).isNotNull();
            assertThat(service.containsKey("ports")).as(file + " host publishing").isFalse();
            assertThat(service.containsKey("network_mode")).as(file + " host/service networking").isFalse();
            assertThat(service.containsKey("labels")).as(file + " implicit ingress labels").isFalse();
        }
        Map<?, ?> services = (Map<?, ?>) yaml("compose.prod.yml").get("services");
        Map<?, ?> config = (Map<?, ?>) services.get("config-server");
        Map<?, ?> environment = (Map<?, ?>) config.get("environment");
        assertThat(environment.get("SPRING_CLOUD_CONFIG_SERVER_ENCRYPT_ENABLED")).isEqualTo("false");
        assertThat(environment.containsKey("CONFIG_CRYPTO_ENDPOINT_TOKEN")).isFalse();
        for (String file : List.of("compose.external-dev.yml", "compose.self-contained.yml")) {
            Map<?, ?> overlayServices = (Map<?, ?>) yaml(file).get("services");
            assertThat(overlayServices.containsKey("config-server")).as(file + " must not reopen Config ingress").isFalse();
        }
    }

    @Test
    void publicGatewayRoutesNeverForwardToConfigServer() throws IOException {
        for (String file : List.of("config-repo/gateway-service.yml", "gateway/src/main/resources/application.yml",
                "gateway/src/main/resources/application-local.yml")) {
            YamlPropertiesFactoryBean loader = new YamlPropertiesFactoryBean();
            loader.setResources(new FileSystemResource(root.resolve(file)));
            Properties properties = loader.getObject();
            for (String key : properties.stringPropertyNames()) {
                if (key.startsWith("spring.cloud.gateway.routes[") && key.endsWith(".uri")) {
                    String uri = properties.getProperty(key).toLowerCase();
                    assertThat(uri.contains("config-server") || uri.contains(":8888"))
                            .as(file + " Config Server route").isFalse();
                }
            }
        }
    }

    @Test
    void defaultPolicyDisablesCryptoAndPayloadErrorLogging() {
        YamlPropertiesFactoryBean loader = new YamlPropertiesFactoryBean();
        loader.setResources(new FileSystemResource(root.resolve("config-server/src/main/resources/application.yml")));
        Properties properties = loader.getObject();
        assertThat(properties.getProperty("spring.cloud.config.server.encrypt.enabled")).isEqualTo("false");
        assertThat(properties.getProperty(
                "logging.level.org.springframework.cloud.config.server.encryption.EncryptionController"))
                .isEqualTo("OFF");
        assertThat(properties.containsKey("config.crypto.endpoint.token")).isFalse();
        assertThat(properties.containsKey("config-server.internal-crypto-token")).isFalse();
    }

    private Map<?, ?> yaml(String file) throws IOException {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setMaxAliasesForCollections(1000);
        try (InputStream input = Files.newInputStream(root.resolve(file))) {
            return new Yaml(new SafeConstructor(options)).load(input);
        }
    }
}
