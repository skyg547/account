package com.ho.account.configserver.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * ==============================================================================
 * Encryption Endpoint Security Filter (/encrypt, /decrypt 엔드포인트 보호 필터)
 * ==============================================================================
 * [Architecture & Security Contract]
 * 1. Fail-Closed Endpoint-Disabled Default:
 *    - 기본적으로 'spring.cloud.config.server.encrypt.enabled'는 false로 설정됩니다.
 *    - 비인가자 또는 외부 네트워크에서 /encrypt, /decrypt 호출 시 404 (Not Found)를 반환하여
 *      엔드포인트 자체를 외부에 노출하지 않습니다.
 *
 * 2. Authenticated Internal Access Control:
 *    - 만약 내부 운영 목적 등으로 활성화된 경우, 오직 사전 승인되어 주입된 내부 인증 토큰
 *      ('X-Config-Internal-Token')과 일치하는 요청만 허용하고,
 *      누락되거나 불일치하는 익명/위조 요청은 401 Unauthorized로 fail-closed 차단합니다.
 *
 * 3. Health & Config Resolution Passthrough:
 *    - /actuator/** (헬스체크/readiness) 및 /{name}/{profile} (클라이언트 설정 조회) 경로는
 *      어떠한 간섭 없이 투명하게 패스스루(Passthrough)되어 무회귀를 보장합니다.
 * ==============================================================================
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EncryptionEndpointSecurityFilter extends OncePerRequestFilter {

    private final boolean encryptEnabled;
    private final String internalToken;

    public EncryptionEndpointSecurityFilter(
            @Value("${spring.cloud.config.server.encrypt.enabled:false}") boolean encryptEnabled,
            @Value("${config-server.internal-crypto-token:}") String internalToken) {
        this.encryptEnabled = encryptEnabled;
        this.internalToken = internalToken != null ? internalToken.trim() : "";
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (isEncryptionPath(path)) {
            if (!encryptEnabled) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Encryption endpoints are disabled under fail-closed security policy.");
                return;
            }

            if (internalToken.isEmpty()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Internal crypto token is not configured.");
                return;
            }

            String providedToken = request.getHeader("X-Config-Internal-Token");
            if (providedToken == null || !internalToken.equals(providedToken.trim())) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized access to encryption endpoint.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isEncryptionPath(String path) {
        if (path == null) {
            return false;
        }
        return path.equals("/encrypt") || path.startsWith("/encrypt/")
                || path.equals("/decrypt") || path.startsWith("/decrypt/");
    }
}