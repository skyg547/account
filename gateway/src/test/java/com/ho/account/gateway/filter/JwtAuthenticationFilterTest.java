package com.ho.account.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.gateway.config.RateLimiterConfig;
import com.ho.account.gateway.security.AccessTokenVerifier;
import com.ho.account.gateway.security.AuthenticatedPrincipal;
import com.ho.account.gateway.security.TokenVersionValidationResult;
import com.ho.account.gateway.security.TokenVersionValidator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class JwtAuthenticationFilterTest {

    @Test
    void loginIsPublicButClientSuppliedIdentityHeadersAreRemoved() {
        JwtAuthenticationFilter filter = filterThatMustNotVerify();
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.post("/api/auth/login")
                .header(JwtAuthenticationFilter.AUTH_USER_HEADER, "attacker")
                .header(JwtAuthenticationFilter.USER_ID_HEADER, "attacker")
                .header(JwtAuthenticationFilter.AUTH_DEPARTMENT_HEADER, "FAKE"));

        StepVerifier.create(filter.filter(exchange, capture(forwarded))).verifyComplete();

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getRequest().getHeaders().containsKey(JwtAuthenticationFilter.AUTH_USER_HEADER))
                .isFalse();
        assertThat(forwarded.get().getRequest().getHeaders().containsKey(JwtAuthenticationFilter.USER_ID_HEADER))
                .isFalse();
        assertThat(forwarded.get().getRequest().getHeaders().containsKey(JwtAuthenticationFilter.AUTH_DEPARTMENT_HEADER))
                .isFalse();
    }

    @Test
    void internalAuthCallbackIsNotExposedEvenWhenRouteConfigurationChanges() {
        JwtAuthenticationFilter filter = filterThatMustNotVerify();
        AtomicBoolean chained = new AtomicBoolean();
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.post(
                "/api/auth/internal/users/admin/role-assignments"));

        StepVerifier.create(filter.filter(exchange, ignored -> {
            chained.set(true);
            return Mono.empty();
        })).verifyComplete();

        assertThat(chained).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exchange.getResponse().getHeaders().getFirst(JwtAuthenticationFilter.AUTH_ERROR_HEADER))
                .isEqualTo("INTERNAL_AUTH_ROUTE_NOT_EXPOSED");
    }

    @Test
    void loginPathIsPublicOnlyForPost() {
        JwtAuthenticationFilter filter = filterThatMustNotVerify();
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/auth/login"));

        StepVerifier.create(filter.filter(exchange, ignored -> Mono.empty())).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    @Test
    void protectedApiRejectsMissingBearerToken() {
        JwtAuthenticationFilter filter = filterThatMustNotVerify();
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/basic/accounts"));

        StepVerifier.create(filter.filter(exchange, ignored -> Mono.empty())).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getFirst(JwtAuthenticationFilter.AUTH_ERROR_HEADER))
                .isEqualTo("BEARER_TOKEN_REQUIRED");
    }

    @Test
    void validTokenReplacesEveryClientIdentityHeaderWithVerifiedSnapshot() {
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                "admin",
                List.of("ACCOUNT_ADMIN", "REPORT_READER"),
                7L,
                null);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                token -> {
                    assertThat(token).isEqualTo("signed-token");
                    return principal;
                },
                (username, version) -> Mono.just(TokenVersionValidationResult.VALID));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/basic/accounts")
                .header(HttpHeaders.AUTHORIZATION, "bearer signed-token")
                .header(JwtAuthenticationFilter.AUTH_USER_HEADER, "attacker")
                .header(JwtAuthenticationFilter.USER_ID_HEADER, "attacker")
                .header(JwtAuthenticationFilter.AUTH_ROLES_HEADER, "SUPER_ADMIN")
                .header(JwtAuthenticationFilter.AUTH_ROLE_VERSION_HEADER, "999")
                .header(JwtAuthenticationFilter.AUTH_DEPARTMENT_HEADER, "FAKE"));

        StepVerifier.create(filter.filter(exchange, capture(forwarded))).verifyComplete();

        HttpHeaders headers = forwarded.get().getRequest().getHeaders();
        assertThat(headers.getFirst(JwtAuthenticationFilter.AUTH_USER_HEADER)).isEqualTo("admin");
        assertThat(headers.getFirst(JwtAuthenticationFilter.USER_ID_HEADER)).isEqualTo("admin");
        assertThat(headers.getFirst(JwtAuthenticationFilter.AUTH_ROLES_HEADER))
                .isEqualTo("ACCOUNT_ADMIN,REPORT_READER");
        assertThat(headers.getFirst(JwtAuthenticationFilter.AUTH_ROLE_VERSION_HEADER)).isEqualTo("7");
        assertThat(headers.containsKey(JwtAuthenticationFilter.AUTH_DEPARTMENT_HEADER)).isFalse();
        assertThat((String) forwarded.get().getAttribute(
                RateLimiterConfig.AUTHENTICATED_PRINCIPAL_ATTRIBUTE)).isEqualTo("admin");
    }

    @Test
    void rejectedRoleVersionReturnsUnauthorizedWithoutCallingBusinessRoute() {
        assertValidationFailure(
                Mono.just(TokenVersionValidationResult.REJECTED),
                HttpStatus.UNAUTHORIZED,
                "TOKEN_ROLE_VERSION_REJECTED");
    }

    @Test
    void authFailureReturnsServiceUnavailableInsteadOfMisleadingReloginResponse() {
        assertValidationFailure(
                Mono.just(TokenVersionValidationResult.UNAVAILABLE),
                HttpStatus.SERVICE_UNAVAILABLE,
                "AUTH_VALIDATION_UNAVAILABLE");
    }

    @Test
    void emptyValidationPublisherFailsClosed() {
        assertValidationFailure(
                Mono.empty(),
                HttpStatus.SERVICE_UNAVAILABLE,
                "AUTH_VALIDATION_UNAVAILABLE");
    }

    @Test
    void corsPreflightDoesNotRequireJwt() {
        JwtAuthenticationFilter filter = filterThatMustNotVerify();
        AtomicBoolean chained = new AtomicBoolean();
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.options("/api/basic/accounts"));

        StepVerifier.create(filter.filter(exchange, ignored -> {
            chained.set(true);
            return Mono.empty();
        })).verifyComplete();

        assertThat(chained).isTrue();
    }

    private void assertValidationFailure(
            Mono<TokenVersionValidationResult> validation,
            HttpStatus expectedStatus,
            String expectedErrorCode) {
        AccessTokenVerifier verifier = token -> new AuthenticatedPrincipal(
                "admin", List.of("ACCOUNT_ADMIN"), 3L, "FINANCE");
        TokenVersionValidator validator = (username, version) -> validation;
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(verifier, validator);
        AtomicBoolean chained = new AtomicBoolean();
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/basic/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer signed-token"));

        StepVerifier.create(filter.filter(exchange, ignored -> {
            chained.set(true);
            return Mono.empty();
        })).verifyComplete();

        assertThat(chained).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(expectedStatus);
        assertThat(exchange.getResponse().getHeaders().getFirst(JwtAuthenticationFilter.AUTH_ERROR_HEADER))
                .isEqualTo(expectedErrorCode);
    }

    private JwtAuthenticationFilter filterThatMustNotVerify() {
        return new JwtAuthenticationFilter(
                token -> {
                    throw new AssertionError("token verification must not be called");
                },
                (username, version) -> Mono.error(new AssertionError("version validation must not be called")));
    }

    private GatewayFilterChain capture(AtomicReference<ServerWebExchange> forwarded) {
        return exchange -> {
            forwarded.set(exchange);
            return Mono.empty();
        };
    }

    private MockServerWebExchange exchange(MockServerHttpRequest.BaseBuilder<?> request) {
        return MockServerWebExchange.from(request.build());
    }
}
