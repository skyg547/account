package com.ho.account.internalaudit.api.adapter.in.web;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

/**
 * Authenticates each Internal Audit HTTP request independently of Gateway headers. A signed token
 * still needs a live Auth role-version check and an exact module function grant before reaching a use case.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public final class InternalAuditIdentityFilter extends OncePerRequestFilter {
    public static final String PRINCIPAL_ATTRIBUTE = InternalAuditIdentityFilter.class.getName() + ".principal";
    private static final String API_PREFIX = "/api/v1/internalaudit/";

    private final SecretKey signingKey;
    private final String issuer;
    private final RestClient authClient;
    private final InternalAuditPermissionPolicy permissionPolicy;

    public InternalAuditIdentityFilter(
            @Value("${internal-audit.identity.jwt-secret:}") String secret,
            @Value("${internal-audit.identity.jwt-issuer:auth-service}") String issuer,
            @Value("${internal-audit.identity.auth-base-url:}") String authBaseUrl,
            InternalAuditPermissionPolicy permissionPolicy) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.signingKey = keyBytes.length >= 32 ? Keys.hmacShaKeyFor(keyBytes) : null;
        this.issuer = issuer;
        this.authClient = client(authBaseUrl);
        this.permissionPolicy = permissionPolicy;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(API_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpServletRequest sanitized = new SanitizedRequest(request);
        String token = bearerToken(sanitized);
        if (token == null) {
            reject(response, HttpStatus.UNAUTHORIZED, "BEARER_TOKEN_REQUIRED");
            return;
        }
        if (signingKey == null || issuer == null || issuer.isBlank()) {
            reject(response, HttpStatus.SERVICE_UNAVAILABLE, "IDENTITY_VERIFICATION_UNAVAILABLE");
            return;
        }

        final InternalAuditPrincipal principal;
        try {
            Jws<Claims> jwt = Jwts.parserBuilder().setSigningKey(signingKey).requireIssuer(issuer).build()
                    .parseClaimsJws(token);
            if (!"HS256".equals(jwt.getHeader().getAlgorithm())) {
                throw new IllegalArgumentException("Unsupported JWT algorithm");
            }
            Claims claims = jwt.getBody();
            validateTime(claims);
            principal = new InternalAuditPrincipal(
                    claims.getSubject(), resolveRoles(claims), resolveRoleVersion(claims));
        } catch (RuntimeException exception) {
            reject(response, HttpStatus.UNAUTHORIZED, "ACCESS_TOKEN_INVALID");
            return;
        }

        if (authClient == null) {
            reject(response, HttpStatus.SERVICE_UNAVAILABLE, "AUTH_VALIDATION_UNAVAILABLE");
            return;
        }
        final TokenVersionResponse version;
        try {
            // Check Auth on every request so a role or account change invalidates an otherwise signed token.
            version = authClient.post().uri("/api/auth/validate-token-version")
                    .body(Map.of("username", principal.username(), "roleVersion", principal.roleVersion()))
                    .retrieve().body(TokenVersionResponse.class);
        } catch (RuntimeException exception) {
            reject(response, HttpStatus.SERVICE_UNAVAILABLE, "AUTH_VALIDATION_UNAVAILABLE");
            return;
        }
        if (version == null || version.valid() == null) {
            reject(response, HttpStatus.SERVICE_UNAVAILABLE, "AUTH_VALIDATION_UNAVAILABLE");
            return;
        }
        if (!version.valid()) {
            reject(response, HttpStatus.UNAUTHORIZED, "TOKEN_ROLE_VERSION_REJECTED");
            return;
        }

        String function = request.getRequestURI().startsWith(API_PREFIX + "rcms/") ? "INTERNAL_AUDIT.RCM"
                : request.getRequestURI().startsWith(API_PREFIX + "evaluations/") ? "INTERNAL_AUDIT.EVALUATION" : null;
        String access = "GET".equals(request.getMethod()) ? "READ"
                : "POST".equals(request.getMethod()) ? "WRITE" : null;
        // Unknown paths and methods have no function grant, even when the caller has an allowed role.
        if (function == null || access == null) {
            reject(response, HttpStatus.FORBIDDEN, "FUNCTION_PERMISSION_DENIED");
            return;
        }
        if (!permissionPolicy.hasPermission(principal, function, access)) {
            reject(response, HttpStatus.FORBIDDEN, "FUNCTION_PERMISSION_DENIED");
            return;
        }

        sanitized.setAttribute(PRINCIPAL_ATTRIBUTE, principal);
        try {
            chain.doFilter(sanitized, response);
        } finally {
            sanitized.removeAttribute(PRINCIPAL_ATTRIBUTE);
        }
    }

    /** Controllers accept only the filter-created principal; caller-supplied headers are never a fallback. */
    static String requireActor() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                && attributes.getRequest().getAttribute(PRINCIPAL_ATTRIBUTE) instanceof InternalAuditPrincipal principal) {
            return principal.username();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Validated identity is required.");
    }

    private static RestClient client(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2000);
        requestFactory.setReadTimeout(2000);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    private static void validateTime(Claims claims) {
        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();
        Instant now = Instant.now();
        if (issuedAt == null || expiration == null || !expiration.after(issuedAt)
                || issuedAt.toInstant().isAfter(now) || !expiration.toInstant().isAfter(now)) {
            throw new IllegalArgumentException("Invalid token time claims");
        }
    }

    private static List<String> resolveRoles(Claims claims) {
        if (!(claims.get("roles") instanceof List<?> values)) {
            throw new IllegalArgumentException("Invalid roles claim");
        }
        return values.stream().map(value -> {
            if (!(value instanceof String role)) {
                throw new IllegalArgumentException("Invalid role");
            }
            return role;
        }).toList();
    }

    private static long resolveRoleVersion(Claims claims) {
        if (!(claims.get("roleVersion") instanceof Number number)) {
            throw new IllegalArgumentException("Invalid role version claim");
        }
        return new BigDecimal(number.toString()).longValueExact();
    }

    private static String bearerToken(HttpServletRequest request) {
        Enumeration<String> headers = request.getHeaders("Authorization");
        if (headers == null || !headers.hasMoreElements()) {
            return null;
        }
        String authorization = headers.nextElement();
        if (headers.hasMoreElements() || authorization == null || authorization.length() <= 7
                || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private static void reject(HttpServletResponse response, HttpStatus status, String code) {
        response.setStatus(status.value());
        response.setHeader("X-Auth-Error", code);
    }

    private record TokenVersionResponse(Boolean valid, String reason) {
    }

    private static final class SanitizedRequest extends HttpServletRequestWrapper {
        private SanitizedRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getHeader(String name) {
            return isIdentityHeader(name) ? null : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return isIdentityHeader(name) ? Collections.emptyEnumeration() : super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            return Collections.enumeration(Collections.list(super.getHeaderNames()).stream()
                    .filter(name -> !isIdentityHeader(name)).toList());
        }

        private static boolean isIdentityHeader(String name) {
            String normalized = name.toLowerCase(Locale.ROOT);
            return normalized.startsWith("x-auth-") || normalized.equals("x-user-id")
                    || normalized.equals("x-service-identity") || normalized.equals("x-internal-token")
                    || normalized.equals("x-service-name");
        }
    }
}
