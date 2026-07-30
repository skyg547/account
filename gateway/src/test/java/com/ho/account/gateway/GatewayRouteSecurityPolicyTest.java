package com.ho.account.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.gateway.web.FallbackController;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;

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

    @Test
    void masterDataPathsUseExplicitRouteBeforeLegacyCatchAll() {
        Properties properties = loadGatewayProperties();
        int masterDataRouteIndex = routeIndex(properties, "master-data-api");
        int legacyCatchAllRouteIndex = routeIndex(properties, "account-api");

        assertThat(masterDataRouteIndex).isGreaterThanOrEqualTo(0);
        assertThat(legacyCatchAllRouteIndex).isGreaterThan(masterDataRouteIndex);
        assertThat(properties.getProperty(
                        "spring.cloud.gateway.routes[" + masterDataRouteIndex + "].predicates[0]"))
                .isEqualTo("Path=/api/basic/**,/api/master-data/**");
        assertThat(properties.getProperty(
                        "spring.cloud.gateway.routes[" + masterDataRouteIndex + "].filters[0].name"))
                .isEqualTo("CircuitBreaker");
        assertThat(properties.getProperty(
                        "spring.cloud.gateway.routes[" + masterDataRouteIndex + "].filters[0].args.name"))
                .isEqualTo("masterDataCircuitBreaker");
        assertThat(properties.getProperty(
                        "spring.cloud.gateway.routes[" + legacyCatchAllRouteIndex + "].predicates[0]"))
                .isEqualTo("Path=/api/**");
    }

    @Test
    void closingPathsUseDedicatedCircuitBreakerRouteBeforeLegacyCatchAll() {
        Properties properties = loadGatewayProperties();
        int closingRouteIndex = routeIndex(properties, "closing-api");
        int legacyCatchAllRouteIndex = routeIndex(properties, "account-api");

        assertThat(closingRouteIndex).isGreaterThanOrEqualTo(0);
        assertThat(legacyCatchAllRouteIndex).isGreaterThan(closingRouteIndex);
        assertThat(properties.getProperty(
                        "spring.cloud.gateway.routes[" + closingRouteIndex + "].uri"))
                .isEqualTo("lb://closing-service");
        assertThat(properties.getProperty(
                        "spring.cloud.gateway.routes[" + closingRouteIndex + "].predicates[0]"))
                .isEqualTo("Path=/api/closing/**");
        assertThat(properties.getProperty(
                        "spring.cloud.gateway.routes[" + closingRouteIndex + "].filters[0].name"))
                .isEqualTo("CircuitBreaker");
        assertThat(properties.getProperty(
                        "spring.cloud.gateway.routes[" + closingRouteIndex + "].filters[0].args.name"))
                .isEqualTo("closingCircuitBreaker");
        assertThat(properties.getProperty(
                        "spring.cloud.gateway.routes[" + closingRouteIndex + "].filters[0].args.fallbackUri"))
                .isEqualTo("forward:/fallback/closing");
    }

    @Test
    void budgetPathsUseDedicatedCircuitBreakerRouteBeforeLegacyCatchAll() {
        Properties properties = loadGatewayProperties();
        int budgetRouteIndex = routeIndex(properties, "budget-api");
        int legacyCatchAllRouteIndex = routeIndex(properties, "account-api");

        assertThat(budgetRouteIndex).isGreaterThanOrEqualTo(0);
        assertThat(legacyCatchAllRouteIndex).isGreaterThan(budgetRouteIndex);
        assertThat(properties.getProperty(
                        "spring.cloud.gateway.routes[" + budgetRouteIndex + "].uri"))
                .isEqualTo("lb://budget-api");
        assertThat(properties.getProperty(
                        "spring.cloud.gateway.routes[" + budgetRouteIndex + "].predicates[0]"))
                .isEqualTo("Path=/api/budgets/**");
        assertThat(properties.getProperty(
                        "spring.cloud.gateway.routes[" + budgetRouteIndex + "].filters[0].name"))
                .isEqualTo("CircuitBreaker");
        assertThat(properties.getProperty(
                        "spring.cloud.gateway.routes[" + budgetRouteIndex + "].filters[0].args.name"))
                .isEqualTo("budgetCircuitBreaker");
        assertThat(properties.getProperty(
                        "spring.cloud.gateway.routes[" + budgetRouteIndex + "].filters[0].args.fallbackUri"))
                .isEqualTo("forward:/fallback/budget");
    }

    @Test
    void budgetFallbackReturnsServiceUnavailableWithoutPretendingToProcessBudgetWork() {
        ResponseEntity<Map<String, Object>> response = Objects.requireNonNull(
                new FallbackController().budgetFallback().block());

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody())
                .containsEntry("status", 503)
                .containsKey("message");
    }

    private int routeIndex(Properties properties, String routeId) {
        for (int index = 0; index < properties.size(); index++) {
            if (routeId.equals(properties.getProperty(
                    "spring.cloud.gateway.routes[" + index + "].id"))) {
                return index;
            }
        }
        return -1;
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
