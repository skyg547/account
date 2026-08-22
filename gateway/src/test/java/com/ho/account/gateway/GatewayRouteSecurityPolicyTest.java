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

    @Test
    void internalAuditPathsUseDedicatedCircuitBreakerRouteBeforeLegacyCatchAll() {
        Properties properties = loadGatewayProperties();
        int routeIndex = routeIndex(properties, "internal-audit-api");
        int legacyCatchAllRouteIndex = routeIndex(properties, "account-api");

        assertThat(routeIndex).isGreaterThanOrEqualTo(0);
        assertThat(legacyCatchAllRouteIndex).isGreaterThan(routeIndex);
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].uri"))
                .isEqualTo("lb://internal-audit-service");
        assertThat(properties.getProperty(
                        "spring.cloud.gateway.routes[" + routeIndex + "].predicates[0]"))
                .isEqualTo("Path=/api/v1/internalaudit/**");
        assertThat(properties.getProperty(
                        "spring.cloud.gateway.routes[" + routeIndex + "].filters[0].args.fallbackUri"))
                .isEqualTo("forward:/fallback/internal-audit");
    }

    @Test
    void internalAuditFallbackReturnsServiceUnavailable() {
        ResponseEntity<Map<String, Object>> response = Objects.requireNonNull(
                new FallbackController().internalAuditFallback().block());

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody())
                .containsEntry("status", 503)
                .containsKey("message");
    }

    @Test
    void explicitRoutesUseEurekaClientWithoutDiscoveryGeneratedRoutes() {
        Properties packagedProperties = loadYamlProperties(
                repositoryRoot().resolve("gateway/src/main/resources/application.yml"));
        Properties externalProperties = loadGatewayProperties();

        assertThat(packagedProperties.getProperty("spring.cloud.gateway.discovery.locator.enabled"))
                .isEqualTo("false");
        assertThat(externalProperties.getProperty("spring.cloud.gateway.discovery.locator.enabled"))
                .isEqualTo("false");
        assertThat(packagedProperties)
                .doesNotContainKeys(
                        "spring.cloud.gateway.discovery.locator.lower-case-service-id",
                        "spring.cloud.gateway.discovery.locator.lower-case-service-id-with-rest-site");
        assertThat(externalProperties)
                .doesNotContainKeys(
                        "spring.cloud.gateway.discovery.locator.lower-case-service-id",
                        "spring.cloud.gateway.discovery.locator.lower-case-service-id-with-rest-site");

        assertThat(externalProperties.getProperty("eureka.client.enabled")).isEqualTo("true");
        assertThat(externalProperties.getProperty("eureka.client.register-with-eureka"))
                .isEqualTo("true");
        assertThat(externalProperties.getProperty("eureka.client.fetch-registry"))
                .isEqualTo("true");
        assertRouteUri(externalProperties, "auth-login-api", "lb://auth-service");
        assertRouteUri(externalProperties, "master-data-api", "lb://master-data");
        assertRouteUri(externalProperties, "journal-ledger-api", "lb://journal-ledger");
        assertRouteUri(externalProperties, "closing-api", "lb://closing-service");
        assertRouteUri(externalProperties, "budget-api", "lb://budget-api");
        assertRouteUri(externalProperties, "internal-audit-api", "lb://internal-audit-service");
        assertRouteUri(externalProperties, "openapi-master-data", "lb://master-data");
        assertRouteUri(externalProperties, "openapi-journal-ledger", "lb://journal-ledger");
        assertRouteUri(externalProperties, "openapi-auth", "lb://auth-service");
        assertRouteUri(externalProperties, "account-api", "lb://account");
        assertThat(externalProperties.getProperty("spring.cloud.gateway.default-filters[0].name"))
                .isEqualTo("RequestRateLimiter");
        assertThat(externalProperties.entrySet().stream()
                .anyMatch(e -> e.getKey().toString().contains("globalcors") && e.getValue().toString().contains("3600")))
                .isTrue();
    }

    private void assertRouteUri(Properties properties, String routeId, String expectedUri) {
        int index = routeIndex(properties, routeId);

        assertThat(index).as("route index for %s", routeId).isGreaterThanOrEqualTo(0);
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + index + "].uri"))
                .as("route URI for %s", routeId)
                .isEqualTo(expectedUri);
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
        return loadYamlProperties(repositoryRoot().resolve("config-repo/gateway-service.yml"));
    }

    private Properties loadYamlProperties(Path configurationPath) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new FileSystemResource(configurationPath));
        Properties properties = factory.getObject();
        if (properties == null) {
            throw new IllegalStateException(configurationPath + " could not be loaded");
        }
        return properties;
    }

    private Path repositoryRoot() {
        Path candidate = Path.of("").toAbsolutePath();
        while (candidate != null && !Files.exists(candidate.resolve("settings.gradle"))) {
            candidate = candidate.getParent();
        }
        if (candidate == null) {
            throw new IllegalStateException("repository root was not found");
        }
        return candidate;
    }
}
