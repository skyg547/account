package com.ho.account.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;

class GatewayRouteSecurityPolicyTest {

    @Test
    void externalConfigurationExposesOnlyLoginUnderPublicAuthRoute() {
        Properties properties = loadGatewayProperties();

        assertThat(properties.getProperty("server.port")).isEqualTo("8000");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[0].id"))
                .isEqualTo("auth-login-api");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[0].predicates[0]"))
                .isEqualTo("Path=/api/auth/login");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[0].predicates[1]"))
                .isEqualTo("Method=POST");
        assertThat(properties.values())
                .doesNotContain("Path=/api/auth/**")
                .doesNotContain("JwtAuthenticationFilter");
    }

    private Properties loadGatewayProperties() {
        Path configurationPath = resolveConfigurationPath();
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new FileSystemResource(configurationPath));
        Properties properties = factory.getObject();
        if (properties == null) {
            throw new IllegalStateException("gateway-service.yml could not be loaded");
        }
        return properties;
    }

    private Path resolveConfigurationPath() {
        Path rootWorkingDirectory = Path.of("config-repo", "gateway-service.yml");
        if (Files.exists(rootWorkingDirectory)) {
            return rootWorkingDirectory;
        }
        Path moduleWorkingDirectory = Path.of("..", "config-repo", "gateway-service.yml");
        if (Files.exists(moduleWorkingDirectory)) {
            return moduleWorkingDirectory;
        }
        throw new IllegalStateException("config-repo/gateway-service.yml was not found");
    }
}
