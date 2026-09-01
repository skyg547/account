package com.ho.account.gateway.filter;

import com.ho.account.gateway.config.RateLimiterConfig;
import com.ho.account.gateway.security.AccessTokenVerifier;
import com.ho.account.gateway.security.AuthenticatedPrincipal;
import com.ho.account.gateway.security.InvalidAccessTokenException;
import com.ho.account.gateway.security.TokenVersionValidationResult;
import com.ho.account.gateway.security.TokenVersionValidator;
import java.util.List;
import java.util.Optional;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * JWT 검문소 (Gateway 전역 필터)
 * 대문(Gateway)을 통과하는 API 요청의 출입증을 검사하고 검증된 내부 신원 헤더만 다시 만듭니다.
 *
 * <p>초보자/업무 흐름: 클라이언트 헤더 제거 -> 공개/내부 경로 정책 확인 -> JWT 검증 -> Auth의
 * roleVersion 확인 -> 신뢰 가능한 {@code X-Auth-*} 헤더 생성 -> 뒤쪽 업무 서비스 전달 순서입니다.
 * 라우트마다 필터를 빠뜨릴 수 없도록 모든 {@code /api/**} 요청에 기본 적용합니다.</p>
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    static final String AUTH_ERROR_HEADER = "X-Auth-Error";
    static final String AUTH_USER_HEADER = "X-Auth-User";
    static final String AUTH_ROLES_HEADER = "X-Auth-Roles";
    static final String AUTH_ROLE_VERSION_HEADER = "X-Auth-Role-Version";
    static final String AUTH_DEPARTMENT_HEADER = "X-Auth-Department";
    static final String USER_ID_HEADER = "X-User-ID";

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String TOKEN_VERSION_PATH = "/api/auth/validate-token-version";
    private static final String INTERNAL_AUTH_PATH_PREFIX = "/api/auth/internal/";
    private static final String INTERNAL_PATH_PREFIX = "/api/internal/";
    private static final List<String> TRUSTED_IDENTITY_HEADERS = List.of(
            AUTH_USER_HEADER,
            AUTH_ROLES_HEADER,
            AUTH_ROLE_VERSION_HEADER,
            AUTH_DEPARTMENT_HEADER,
            USER_ID_HEADER,
            "X-Service-Identity",
            "X-Internal-Token",
            "X-Service-Name");

    private final AccessTokenVerifier accessTokenVerifier;
    private final TokenVersionValidator tokenVersionValidator;

    public JwtAuthenticationFilter(
            AccessTokenVerifier accessTokenVerifier,
            TokenVersionValidator tokenVersionValidator) {
        this.accessTokenVerifier = accessTokenVerifier;
        this.tokenVersionValidator = tokenVersionValidator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerWebExchange sanitizedExchange = removeUntrustedIdentityHeaders(exchange);
        String path = sanitizedExchange.getRequest().getURI().getPath();

        if (isInternalAuthPath(path)) {
            return onError(sanitizedExchange.getResponse(), "INTERNAL_AUTH_ROUTE_NOT_EXPOSED", HttpStatus.NOT_FOUND);
        }
        if (isPublicRequest(sanitizedExchange.getRequest(), path) || !isApiPath(path)) {
            return chain.filter(sanitizedExchange);
        }

        Optional<String> token = resolveBearerToken(sanitizedExchange.getRequest());
        if (token.isEmpty()) {
            return onError(sanitizedExchange.getResponse(), "BEARER_TOKEN_REQUIRED", HttpStatus.UNAUTHORIZED);
        }

        final AuthenticatedPrincipal principal;
        try {
            principal = accessTokenVerifier.verify(token.orElseThrow());
        } catch (InvalidAccessTokenException | IllegalArgumentException exception) {
            return onError(sanitizedExchange.getResponse(), "ACCESS_TOKEN_INVALID", HttpStatus.UNAUTHORIZED);
        }

        return Mono.defer(() -> tokenVersionValidator.validate(principal.username(), principal.roleVersion()))
                .switchIfEmpty(Mono.just(TokenVersionValidationResult.UNAVAILABLE))
                .onErrorReturn(TokenVersionValidationResult.UNAVAILABLE)
                .flatMap(result -> handleValidationResult(sanitizedExchange, chain, principal, result));
    }

    private Mono<Void> handleValidationResult(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            AuthenticatedPrincipal principal,
            TokenVersionValidationResult result) {
        return switch (result) {
            case VALID -> chain.filter(addAuthenticatedPrincipal(exchange, principal));
            case REJECTED -> onError(
                    exchange.getResponse(),
                    "TOKEN_ROLE_VERSION_REJECTED",
                    HttpStatus.UNAUTHORIZED);
            case UNAVAILABLE -> onError(
                    exchange.getResponse(),
                    "AUTH_VALIDATION_UNAVAILABLE",
                    HttpStatus.SERVICE_UNAVAILABLE);
        };
    }

    private ServerWebExchange removeUntrustedIdentityHeaders(ServerWebExchange exchange) {
        ServerHttpRequest sanitizedRequest = exchange.getRequest().mutate()
                .headers(headers -> TRUSTED_IDENTITY_HEADERS.forEach(headers::remove))
                .build();
        return exchange.mutate().request(sanitizedRequest).build();
    }

    private ServerWebExchange addAuthenticatedPrincipal(
            ServerWebExchange exchange,
            AuthenticatedPrincipal principal) {
        ServerHttpRequest authenticatedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.set(AUTH_USER_HEADER, principal.username());
                    headers.set(USER_ID_HEADER, principal.username());
                    headers.set(AUTH_ROLES_HEADER, String.join(",", principal.roles()));
                    headers.set(AUTH_ROLE_VERSION_HEADER, Long.toString(principal.roleVersion()));
                    if (principal.departmentCode() != null) {
                        headers.set(AUTH_DEPARTMENT_HEADER, principal.departmentCode());
                    }
                })
                .build();
        ServerWebExchange authenticatedExchange = exchange.mutate().request(authenticatedRequest).build();
        authenticatedExchange.getAttributes().put(
                RateLimiterConfig.AUTHENTICATED_PRINCIPAL_ATTRIBUTE,
                principal.username());
        return authenticatedExchange;
    }

    private Optional<String> resolveBearerToken(ServerHttpRequest request) {
        List<String> authorizationHeaders = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (authorizationHeaders == null || authorizationHeaders.size() != 1) {
            return Optional.empty();
        }
        String authorization = authorizationHeaders.get(0);
        if (authorization == null
                || authorization.length() <= 7
                || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return Optional.empty();
        }
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }

    private boolean isPublicRequest(ServerHttpRequest request, String path) {
        return request.getMethod() == HttpMethod.OPTIONS
                || (request.getMethod() == HttpMethod.POST && LOGIN_PATH.equals(path));
    }

    private boolean isApiPath(String path) {
        return "/api".equals(path) || path.startsWith("/api/");
    }

    private boolean isInternalAuthPath(String path) {
        return TOKEN_VERSION_PATH.equals(path)
                || path.startsWith(INTERNAL_AUTH_PATH_PREFIX)
                || path.startsWith(INTERNAL_PATH_PREFIX);
    }

    private Mono<Void> onError(ServerHttpResponse response, String errorCode, HttpStatus status) {
        response.setStatusCode(status);
        response.getHeaders().set(AUTH_ERROR_HEADER, errorCode);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
