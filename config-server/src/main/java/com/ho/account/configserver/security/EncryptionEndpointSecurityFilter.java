package com.ho.account.configserver.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * ==============================================================================
 * Encryption Endpoint Security Filter (/encrypt, /decrypt 엔드포인트 보안 필터)
 * ==============================================================================
 * [Architecture & Security Contract]
 * 1. Fail-Closed Endpoint-Disabled Default:
 *    - 기본적으로 'spring.cloud.config.server.encrypt.enabled'는 false로 설정됩니다.
 *    - 엔드포인트 비활성화 상태에서는 /encrypt, /decrypt 호출 시 404 (Not Found)를 반환하여
 *      엔드포인트 존재 여부 및 동작을 외부에 노출하지 않습니다.
 *
 * 2. External Token Binding & Fail-Closed Authentication:
 *    - 엔드포인트가 활성화된 경우라도 외부에서 주입된 토큰('config.crypto.endpoint.token' 또는
 *      'config-server.internal-crypto-token')이 비어있으면 403 (Forbidden)으로 fail-closed 차단합니다.
 *    - 토큰이 설정된 경우 'X-Config-Token', 'X-Config-Internal-Token', 또는
 *      'Authorization: Bearer <token>' 헤더를 통해 전달된 토큰과 상수 시간(constant-time) 비교를 수행하며,
 *      누락되거나 불일치하는 익명/위조 요청은 401 (Unauthorized)로 차단합니다.
 *
 * 3. Health & Config Resolution Passthrough (Zero Regression):
 *    - /actuator/** (헬스체크, readiness, info 등) 및 /{name}/{profile} (클라이언트 설정 조회) 경로는
 *      어떠한 간섭 없이 투명하게 필터를 통과(Passthrough)하여 비즈니스/인프라 무회귀를 보장합니다.
 * ==============================================================================
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EncryptionEndpointSecurityFilter extends OncePerRequestFilter {

    public static final String CONFIG_TOKEN_HEADER = "X-Config-Token";
    public static final String INTERNAL_CONFIG_TOKEN_HEADER = "X-Config-Internal-Token";

    private final boolean encryptEnabled;
    private final String cryptoToken;

    public EncryptionEndpointSecurityFilter(
            @Value("${spring.cloud.config.server.encrypt.enabled:false}") boolean encryptEnabled,
            @Value("${config.crypto.endpoint.token:${config-server.internal-crypto-token:}}") String cryptoToken) {
        this.encryptEnabled = encryptEnabled;
        this.cryptoToken = cryptoToken != null ? cryptoToken.trim() : "";
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = resolveRequestPath(request);
        if (isEncryptionPath(path)) {
            if (!encryptEnabled) {
                response.sendError(
                        HttpServletResponse.SC_NOT_FOUND,
                        "Encryption endpoints are disabled under fail-closed security policy.");
                return;
            }

            if (cryptoToken.isEmpty()) {
                response.sendError(
                        HttpServletResponse.SC_FORBIDDEN,
                        "Internal crypto token is not configured.");
                return;
            }

            String providedToken = extractProvidedToken(request);
            if (providedToken == null || !isValidToken(cryptoToken, providedToken)) {
                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Unauthorized access to encryption endpoint.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveRequestPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (requestUri == null) {
            return "";
        }
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    private boolean isEncryptionPath(String path) {
        if (path == null) {
            return false;
        }
        String normalized = path.trim();
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.equals("/encrypt")
                || normalized.startsWith("/encrypt/")
                || normalized.equals("/decrypt")
                || normalized.startsWith("/decrypt/");
    }

    private String extractProvidedToken(HttpServletRequest request) {
        String token = request.getHeader(CONFIG_TOKEN_HEADER);
        if (token != null && !token.isBlank()) {
            return token.trim();
        }

        token = request.getHeader(INTERNAL_CONFIG_TOKEN_HEADER);
        if (token != null && !token.isBlank()) {
            return token.trim();
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String bearerToken = authHeader.substring(7).trim();
            if (!bearerToken.isBlank()) {
                return bearerToken;
            }
        }

        return null;
    }

    private boolean isValidToken(String expectedToken, String providedToken) {
        return MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                providedToken.getBytes(StandardCharsets.UTF_8));
    }
}
