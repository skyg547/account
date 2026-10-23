package com.ho.account.auth.api.web;

import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import com.ho.account.auth.core.application.port.in.AuthUseCase;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriUtils;

/** Validates the forwarded bearer token at the Auth service boundary for every admin request. */
@Component
public class AdminBearerAuthenticationFilter extends OncePerRequestFilter {

    static final String AUTHENTICATED_USER_ATTRIBUTE =
            AdminBearerAuthenticationFilter.class.getName() + ".user";
    static final String AUTHENTICATED_ROLES_ATTRIBUTE =
            AdminBearerAuthenticationFilter.class.getName() + ".roles";

    private final JwtParser jwtParser;
    private final AuthUseCase authUseCase;

    public AdminBearerAuthenticationFilter(AuthModuleProperties properties, AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
        this.jwtParser = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(
                        properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8)))
                .requireIssuer(properties.getJwt().getIssuer())
                .requireAudience(properties.getJwt().getAudience())
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = request.getRequestURI().substring(request.getContextPath().length());
        return "OPTIONS".equals(request.getMethod())
                || (!isAdminPath(request.getServletPath()) && !isAdminPath(requestPath));
    }

    private boolean isAdminPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        String decoded = path;
        // MVC removes matrix parameters and decodes path segments before controller mapping.
        // Match that route shape here so neither representation can skip authentication.
        for (int pass = 0; pass < 2; pass++) {
            try {
                decoded = UriUtils.decode(decoded, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException malformedPath) {
                break;
            }
        }
        String[] segments = decoded.split("/", -1);
        return segments.length >= 3
                && segments[0].isEmpty()
                && "api".equals(beforeMatrixParameter(segments[1]))
                && "admin".equals(beforeMatrixParameter(segments[2]));
    }

    private String beforeMatrixParameter(String segment) {
        int separator = segment.indexOf(';');
        return separator < 0 ? segment : segment.substring(0, separator);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        List<String> roles;
        String username;
        try {
            String token = bearerToken(request);
            Jws<Claims> verified = jwtParser.parseClaimsJws(token);
            Claims claims = verified.getBody();
            if (!"HS256".equals(verified.getHeader().getAlgorithm())
                    || claims.getSubject() == null || claims.getSubject().isBlank()
                    || claims.getIssuedAt() == null || claims.getExpiration() == null
                    || !claims.getExpiration().after(claims.getIssuedAt())
                    || claims.getIssuedAt().after(new Date())) {
                throw new IllegalArgumentException("invalid token claims");
            }
            username = claims.getSubject();
            roles = roles(claims);
            requireMatchingIdentityHeaders(request, claims, roles);
            // Direct Auth calls must honor role revocation just as Gateway-routed calls do.
            if (!authUseCase.validateTokenVersion(claims.getSubject(), roleVersion(claims))) {
                throw new IllegalArgumentException("token version is no longer valid");
            }
        } catch (RuntimeException invalidToken) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setHeader("X-Auth-Error", "ACCESS_TOKEN_INVALID");
            return;
        }
        // Only verified claims become controller authority; forwarded identity headers never do.
        request.setAttribute(AUTHENTICATED_USER_ATTRIBUTE, username);
        request.setAttribute(AUTHENTICATED_ROLES_ATTRIBUTE, roles);
        chain.doFilter(request, response);
    }

    private String bearerToken(HttpServletRequest request) {
        Enumeration<String> values = request.getHeaders(HttpHeaders.AUTHORIZATION);
        if (values == null || !values.hasMoreElements()) {
            throw new IllegalArgumentException("bearer token required");
        }
        String header = values.nextElement();
        if (values.hasMoreElements() || header == null || !header.startsWith("Bearer ")) {
            throw new IllegalArgumentException("one bearer token required");
        }
        String token = header.substring(7);
        if (!token.matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("invalid bearer syntax");
        }
        return token;
    }

    private List<String> roles(Claims claims) {
        Object value = claims.get("roles");
        if (!(value instanceof List<?> values)) {
            throw new IllegalArgumentException("roles are required");
        }
        List<String> roles = new ArrayList<>();
        for (Object role : values) {
            if (!(role instanceof String text) || text.isBlank()) {
                throw new IllegalArgumentException("invalid role");
            }
            roles.add(text);
        }
        return List.copyOf(roles);
    }

    private void requireMatchingIdentityHeaders(HttpServletRequest request, Claims claims, List<String> roles) {
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            String normalized = name.toLowerCase(Locale.ROOT);
            if (!normalized.startsWith("x-auth-") && !normalized.equals("x-user-id")) {
                continue;
            }
            String expected = switch (normalized) {
                case "x-auth-user", "x-user-id" -> claims.getSubject();
                case "x-auth-roles" -> String.join(",", roles);
                case "x-auth-role-version" -> Long.toString(roleVersion(claims));
                case "x-auth-department" -> claims.get("departmentCode", String.class);
                default -> null;
            };
            Enumeration<String> values = request.getHeaders(name);
            if (expected == null || values == null || !values.hasMoreElements()
                    || !expected.equals(values.nextElement()) || values.hasMoreElements()) {
                throw new IllegalArgumentException("identity header does not match token");
            }
        }
    }

    private long roleVersion(Claims claims) {
        Object value = claims.get("roleVersion");
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("role version is required");
        }
        try {
            long version = new java.math.BigDecimal(number.toString()).longValueExact();
            if (version < 0) {
                throw new IllegalArgumentException("role version is invalid");
            }
            return version;
        } catch (ArithmeticException | NumberFormatException invalid) {
            throw new IllegalArgumentException("role version is invalid", invalid);
        }
    }
}
