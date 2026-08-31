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
    void masterDataPathsUseExplicitCircuitBreakerRoute() {
        Properties properties = loadGatewayProperties();
        int masterDataRouteIndex = routeIndex(properties, "master-data-api");

        assertThat(masterDataRouteIndex).isGreaterThanOrEqualTo(0);
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + masterDataRouteIndex + "].uri"))
                .isEqualTo("lb://master-data");
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
                        "spring.cloud.gateway.routes[" + masterDataRouteIndex + "].filters[0].args.fallbackUri"))
                .isEqualTo("forward:/fallback/master-data");
    }

    @Test
    void closingPathsUseDedicatedCircuitBreakerRoute() {
        Properties properties = loadGatewayProperties();
        int closingRouteIndex = routeIndex(properties, "closing-api");

        assertThat(closingRouteIndex).isGreaterThanOrEqualTo(0);
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
    void budgetPathsUseDedicatedCircuitBreakerRoute() {
        Properties properties = loadGatewayProperties();
        int budgetRouteIndex = routeIndex(properties, "budget-api");

        assertThat(budgetRouteIndex).isGreaterThanOrEqualTo(0);
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
    void internalAuditPathsUseDedicatedCircuitBreakerRoute() {
        Properties properties = loadGatewayProperties();
        int routeIndex = routeIndex(properties, "internal-audit-api");

        assertThat(routeIndex).isGreaterThanOrEqualTo(0);
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
    void depositPathsUseDedicatedCircuitBreakerRoute() {
        Properties properties = loadGatewayProperties();
        int routeIndex = routeIndex(properties, "deposit-api");

        assertThat(routeIndex).isGreaterThanOrEqualTo(0);
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].uri"))
                .isEqualTo("lb://deposit-api");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].predicates[0]"))
                .isEqualTo("Path=/api/v1/deposit/**");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].name"))
                .isEqualTo("CircuitBreaker");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].args.name"))
                .isEqualTo("depositCircuitBreaker");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].args.fallbackUri"))
                .isEqualTo("forward:/fallback/deposit");
    }

    @Test
    void depositFallbackReturnsServiceUnavailable() {
        ResponseEntity<Map<String, Object>> response = Objects.requireNonNull(
                new FallbackController().depositFallback().block());

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody())
                .containsEntry("status", 503)
                .containsKey("message");
    }

    @Test
    void receivablePathsUseDedicatedCircuitBreakerRoute() {
        Properties properties = loadGatewayProperties();
        int routeIndex = routeIndex(properties, "receivable-api");

        assertThat(routeIndex).isGreaterThanOrEqualTo(0);
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].uri"))
                .isEqualTo("lb://receivable-api");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].predicates[0]"))
                .isEqualTo("Path=/api/v1/receivable/**");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].name"))
                .isEqualTo("CircuitBreaker");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].args.name"))
                .isEqualTo("receivableCircuitBreaker");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].args.fallbackUri"))
                .isEqualTo("forward:/fallback/receivable");
    }

    @Test
    void receivableFallbackReturnsServiceUnavailable() {
        ResponseEntity<Map<String, Object>> response = Objects.requireNonNull(
                new FallbackController().receivableFallback().block());

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody())
                .containsEntry("status", 503)
                .containsKey("message");
    }

    @Test
    void payablePathsUseDedicatedCircuitBreakerRoute() {
        Properties properties = loadGatewayProperties();
        int routeIndex = routeIndex(properties, "payable-api");

        assertThat(routeIndex).isGreaterThanOrEqualTo(0);
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].uri"))
                .isEqualTo("lb://payable-api");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].predicates[0]"))
                .isEqualTo("Path=/api/v1/payable/**");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].name"))
                .isEqualTo("CircuitBreaker");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].args.name"))
                .isEqualTo("payableCircuitBreaker");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].args.fallbackUri"))
                .isEqualTo("forward:/fallback/payable");
    }

    @Test
    void payableFallbackReturnsServiceUnavailable() {
        ResponseEntity<Map<String, Object>> response = Objects.requireNonNull(
                new FallbackController().payableFallback().block());

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody())
                .containsEntry("status", 503)
                .containsKey("message");
    }

    @Test
    void taxPathsUseDedicatedCircuitBreakerRoute() {
        Properties properties = loadGatewayProperties();
        int routeIndex = routeIndex(properties, "tax-api");

        assertThat(routeIndex).isGreaterThanOrEqualTo(0);
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].uri"))
                .isEqualTo("lb://tax-api");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].predicates[0]"))
                .isEqualTo("Path=/api/v1/tax/**");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].name"))
                .isEqualTo("CircuitBreaker");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].args.name"))
                .isEqualTo("taxCircuitBreaker");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].args.fallbackUri"))
                .isEqualTo("forward:/fallback/tax");
    }

    @Test
    void taxFallbackReturnsServiceUnavailable() {
        ResponseEntity<Map<String, Object>> response = Objects.requireNonNull(
                new FallbackController().taxFallback().block());

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody())
                .containsEntry("status", 503)
                .containsKey("message");
    }

    @Test
    void reconciliationPathsUseDedicatedCircuitBreakerRoute() {
        Properties properties = loadGatewayProperties();
        int routeIndex = routeIndex(properties, "reconciliation-api");

        assertThat(routeIndex).isGreaterThanOrEqualTo(0);
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].uri"))
                .isEqualTo("lb://reconciliation-api");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].predicates[0]"))
                .isEqualTo("Path=/api/v1/reconciliation/**");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].name"))
                .isEqualTo("CircuitBreaker");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].args.name"))
                .isEqualTo("reconciliationCircuitBreaker");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].args.fallbackUri"))
                .isEqualTo("forward:/fallback/reconciliation");
    }

    @Test
    void reconciliationFallbackReturnsServiceUnavailable() {
        ResponseEntity<Map<String, Object>> response = Objects.requireNonNull(
                new FallbackController().reconciliationFallback().block());

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody())
                .containsEntry("status", 503)
                .containsKey("message");
    }

    @Test
    void assetLeasePathsUseDedicatedCircuitBreakerRoute() {
        Properties properties = loadGatewayProperties();
        int routeIndex = routeIndex(properties, "asset-lease-api");

        assertThat(routeIndex).isGreaterThanOrEqualTo(0);
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].uri"))
                .isEqualTo("lb://asset-lease-api");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].predicates[0]"))
                .isEqualTo("Path=/api/v1/asset-lease/**");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].name"))
                .isEqualTo("CircuitBreaker");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].args.name"))
                .isEqualTo("assetLeaseCircuitBreaker");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].args.fallbackUri"))
                .isEqualTo("forward:/fallback/asset-lease");
    }

    @Test
    void assetLeaseFallbackReturnsServiceUnavailable() {
        ResponseEntity<Map<String, Object>> response = Objects.requireNonNull(
                new FallbackController().assetLeaseFallback().block());

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody())
                .containsEntry("status", 503)
                .containsKey("message");
    }

    @Test
    void reportingPathsUseDedicatedCircuitBreakerRoute() {
        Properties properties = loadGatewayProperties();
        int routeIndex = routeIndex(properties, "reporting-api");

        assertThat(routeIndex).isGreaterThanOrEqualTo(0);
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].uri"))
                .isEqualTo("lb://reporting-api");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].predicates[0]"))
                .isEqualTo("Path=/api/v1/reporting/**");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].name"))
                .isEqualTo("CircuitBreaker");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].args.name"))
                .isEqualTo("reportingCircuitBreaker");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].args.fallbackUri"))
                .isEqualTo("forward:/fallback/reporting");
    }

    @Test
    void reportingFallbackReturnsServiceUnavailable() {
        ResponseEntity<Map<String, Object>> response = Objects.requireNonNull(
                new FallbackController().reportingFallback().block());

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody())
                .containsEntry("status", 503)
                .containsKey("message");
    }

    @Test
    void expenditureResolutionPathsUseDedicatedCircuitBreakerRoute() {
        Properties properties = loadGatewayProperties();
        int routeIndex = routeIndex(properties, "expenditure-resolution-api");

        assertThat(routeIndex).isGreaterThanOrEqualTo(0);
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].uri"))
                .isEqualTo("lb://expenditure-resolution-api");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].predicates[0]"))
                .isEqualTo("Path=/api/v1/expenditure/**");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].name"))
                .isEqualTo("CircuitBreaker");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].args.name"))
                .isEqualTo("expenditureResolutionCircuitBreaker");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].args.fallbackUri"))
                .isEqualTo("forward:/fallback/expenditure");
    }

    @Test
    void expenditureResolutionFallbackReturnsServiceUnavailable() {
        ResponseEntity<Map<String, Object>> response = Objects.requireNonNull(
                new FallbackController().expenditureFallback().block());

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody())
                .containsEntry("status", 503)
                .containsKey("message");
    }

    @Test
    void accountMartPathsUseDedicatedCircuitBreakerRoute() {
        Properties properties = loadGatewayProperties();
        int routeIndex = routeIndex(properties, "account-mart-api");

        assertThat(routeIndex).isGreaterThanOrEqualTo(0);
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].uri"))
                .isEqualTo("lb://account-mart-api");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].predicates[0]"))
                .isEqualTo("Path=/api/v1/mart/**");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].name"))
                .isEqualTo("CircuitBreaker");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].args.name"))
                .isEqualTo("accountMartCircuitBreaker");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].args.fallbackUri"))
                .isEqualTo("forward:/fallback/mart");
    }

    @Test
    void accountMartFallbackReturnsServiceUnavailable() {
        ResponseEntity<Map<String, Object>> response = Objects.requireNonNull(
                new FallbackController().martFallback().block());

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody())
                .containsEntry("status", 503)
                .containsKey("message");
    }

    @Test
    void eclPathsUseDedicatedCircuitBreakerRoute() {
        Properties properties = loadGatewayProperties();
        int routeIndex = routeIndex(properties, "ecl-api");

        assertThat(routeIndex).isGreaterThanOrEqualTo(0);
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].uri"))
                .isEqualTo("lb://ecl-api");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].predicates[0]"))
                .isEqualTo("Path=/api/v1/ecl/**");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].name"))
                .isEqualTo("CircuitBreaker");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].args.name"))
                .isEqualTo("eclCircuitBreaker");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + routeIndex + "].filters[0].args.fallbackUri"))
                .isEqualTo("forward:/fallback/ecl");
    }

    @Test
    void eclFallbackReturnsServiceUnavailable() {
        ResponseEntity<Map<String, Object>> response = Objects.requireNonNull(
                new FallbackController().eclFallback().block());

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody())
                .containsEntry("status", 503)
                .containsKey("message");
    }

    @Test
    void legacyAccountMonolithCatchAllRouteIsRemoved() {
        Properties properties = loadGatewayProperties();

        assertThat(routeIndex(properties, "account-api")).isEqualTo(-1);
        assertThat(properties.values())
                .doesNotContain("lb://account")
                .doesNotContain("Path=/api/**");
    }

    @Test
    void explicitRoutesUseEurekaClientWithoutDiscoveryGeneratedRoutes() {
        Properties packagedProperties = loadYamlProperties(
                repositoryRoot().resolve("gateway/src/main/resources/application.yml"));
        Properties properties = loadGatewayProperties();

        assertThat(properties.getProperty("spring.cloud.gateway.discovery.locator.enabled"))
                .isEqualTo("false");
        assertThat(properties)
                .doesNotContainKeys(
                        "spring.cloud.gateway.discovery.locator.lower-case-service-id",
                        "spring.cloud.gateway.discovery.locator.lower-case-service-id-with-rest-site");

        assertThat(properties.getProperty("eureka.client.enabled")).isEqualTo("true");
        assertThat(properties.getProperty("eureka.client.register-with-eureka"))
                .isEqualTo("true");
        assertThat(properties.getProperty("eureka.client.fetch-registry"))
                .isEqualTo("true");
        assertRouteUri(properties, "auth-login-api", "lb://auth-service");
        assertRouteUri(properties, "master-data-api", "lb://master-data");
        assertRouteUri(properties, "journal-ledger-api", "lb://journal-ledger");
        assertRouteUri(properties, "closing-api", "lb://closing-service");
        assertRouteUri(properties, "budget-api", "lb://budget-api");
        assertRouteUri(properties, "internal-audit-api", "lb://internal-audit-service");
        assertRouteUri(properties, "deposit-api", "lb://deposit-api");
        assertRouteUri(properties, "receivable-api", "lb://receivable-api");
        assertRouteUri(properties, "payable-api", "lb://payable-api");
        assertRouteUri(properties, "tax-api", "lb://tax-api");
        assertRouteUri(properties, "reconciliation-api", "lb://reconciliation-api");
        assertRouteUri(properties, "asset-lease-api", "lb://asset-lease-api");
        assertRouteUri(properties, "reporting-api", "lb://reporting-api");
        assertRouteUri(properties, "expenditure-resolution-api", "lb://expenditure-resolution-api");
        assertRouteUri(properties, "account-mart-api", "lb://account-mart-api");
        assertRouteUri(properties, "ecl-api", "lb://ecl-api");
        assertRouteUri(properties, "openapi-master-data", "lb://master-data");
        assertRouteUri(properties, "openapi-journal-ledger", "lb://journal-ledger");
        assertRouteUri(properties, "openapi-auth", "lb://auth-service");
        assertThat(routeIndex(properties, "account-api")).isEqualTo(-1);
        assertThat(properties.getProperty("spring.cloud.gateway.default-filters[0].name"))
                .isEqualTo("RequestRateLimiter");
        assertThat(packagedProperties.getProperty(
                        "spring.cloud.gateway.default-filters[0].args.key-resolver"))
                .isEqualTo("#{@requestRateKeyResolver}");
        assertThat(properties.getProperty(
                        "spring.cloud.gateway.default-filters[0].args.key-resolver"))
                .isEqualTo("#{@requestRateKeyResolver}");
        assertThat(packagedProperties.getProperty("spring.cloud.gateway.default-filters[1]"))
                .isEqualTo("RemoveRequestHeader=X-Bff-Rate-Key");
        assertThat(packagedProperties.getProperty("spring.cloud.gateway.default-filters[2]"))
                .isEqualTo("RemoveRequestHeader=X-Bff-Rate-Timestamp");
        assertThat(packagedProperties.getProperty("spring.cloud.gateway.default-filters[3]"))
                .isEqualTo("RemoveRequestHeader=X-Bff-Rate-Signature");
        assertThat(properties.getProperty("spring.cloud.gateway.default-filters[1]"))
                .isEqualTo("RemoveRequestHeader=X-Bff-Rate-Key");
        assertThat(properties.getProperty("spring.cloud.gateway.default-filters[2]"))
                .isEqualTo("RemoveRequestHeader=X-Bff-Rate-Timestamp");
        assertThat(properties.getProperty("spring.cloud.gateway.default-filters[3]"))
                .isEqualTo("RemoveRequestHeader=X-Bff-Rate-Signature");
        assertThat(packagedProperties.getProperty("gateway.bff.shared-secret"))
                .isEqualTo("${BFF_GATEWAY_SHARED_SECRET:}");
        assertThat(properties.getProperty("gateway.bff.shared-secret"))
                .isEqualTo("${BFF_GATEWAY_SHARED_SECRET:}");
        assertThat(properties.entrySet().stream()
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
        return loadYamlProperties(repositoryRoot().resolve("gateway/src/main/resources/application.yml"));
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
