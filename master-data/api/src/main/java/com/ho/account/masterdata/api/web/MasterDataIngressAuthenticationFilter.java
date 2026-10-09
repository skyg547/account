package com.ho.account.masterdata.api.web;

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
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Verifies credentials at this service's HTTP boundary, including direct calls that bypass Gateway. */
@Component
public final class MasterDataIngressAuthenticationFilter extends OncePerRequestFilter {

    public static final String VERIFIED_SERVICE = MasterDataIngressAuthenticationFilter.class.getName() + ".service";
    public static final String VERIFIED_ACTOR = MasterDataIngressAuthenticationFilter.class.getName() + ".actor";
    public static final String VERIFIED_PERIOD_ID = MasterDataIngressAuthenticationFilter.class.getName() + ".periodId";
    public static final String VERIFIED_CLOSING_STATUS = MasterDataIngressAuthenticationFilter.class.getName() + ".closingStatus";
    private static final String INTERNAL_AUDIENCE = "master-data-internal";
    private static final Set<String> DELEGATED_ROLES = Set.of(
            "ROLE_ADMIN", "ROLE_SYSTEM_ADMIN", "ROLE_ACCOUNTING_ADMIN");

    private final String userSecret;
    private final String userIssuer;
    private final String userAudience;
    private final String serviceSecret;

    public MasterDataIngressAuthenticationFilter(
            @Value("${auth.jwt.secret:}") String userSecret,
            @Value("${auth.jwt.issuer:auth-service}") String userIssuer,
            @Value("${auth.jwt.audience:account-api}") String userAudience,
            @Value("${master-data.internal-auth.secret:}") String serviceSecret) {
        this.userSecret = userSecret;
        this.userIssuer = userIssuer;
        this.userAudience = userAudience;
        this.serviceSecret = serviceSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Servlet context paths vary by deployment; MVC mappings are relative to that context.
        String requestUri = request.getRequestURI();
        if (hasAmbiguousPath(requestUri)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unsupported path encoding");
            return;
        }
        String path = requestUri.substring(request.getContextPath().length());
        boolean internal = path.startsWith("/api/internal/");
        // No public mutation exists in this service, so path normalization cannot bypass authentication.
        boolean userProtected = isMutation(request.getMethod())
                || (path.startsWith("/api") && !path.startsWith("/api/basic/"));
        if (HttpMethod.OPTIONS.matches(request.getMethod()) || (!internal && !userProtected)) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest verifiedRequest;
        try {
            verifiedRequest = internal ? verifyInternal(request) : verifyUser(request);
        } catch (RuntimeException exception) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Verified credential required");
            return;
        }
        chain.doFilter(verifiedRequest, response);
    }

    private HttpServletRequest verifyUser(HttpServletRequest request) {
        if (request.getHeader("X-Service-Identity") != null || request.getHeader("X-Service-Assertion") != null
                || request.getHeader("X-Service-Name") != null || request.getHeader("X-Internal-Token") != null) {
            throw new IllegalArgumentException("service headers are not user credentials");
        }
        Claims claims = parse(bearer(request), userSecret, userIssuer, userAudience);
        String actor = safeHeaderValue(claims.getSubject(), 80);
        List<String> roles = roles(claims.get("roles"));
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("roles required");
        }
        if (Collections.list(request.getHeaders("X-Auth-User")).stream().anyMatch(value -> !value.equals(actor))) {
            throw new IllegalArgumentException("forwarded user mismatch");
        }
        if (Collections.list(request.getHeaders("X-Auth-Roles")).stream().anyMatch(value ->
                !Set.copyOf(Arrays.asList(value.split(",", -1))).equals(Set.copyOf(roles)))) {
            throw new IllegalArgumentException("forwarded roles mismatch");
        }
        Object rawVersion = claims.get("roleVersion");
        if (!(rawVersion instanceof Number version) || version.longValue() < 1
                || !Long.toString(version.longValue()).equals(version.toString())) {
            throw new IllegalArgumentException("valid role version required");
        }
        String roleVersion = Long.toString(version.longValue());
        Object rawDepartment = claims.get("departmentCode");
        String department = rawDepartment == null ? null : safeHeaderValue((String) rawDepartment, 100);
        for (String name : Collections.list(request.getHeaderNames())) {
            String lowerName = name.toLowerCase(Locale.ROOT);
            if (lowerName.startsWith("x-auth-")
                    && !Set.of("x-auth-user", "x-auth-roles", "x-auth-role-version", "x-auth-department")
                            .contains(lowerName)) {
                throw new IllegalArgumentException("unrecognized authority header");
            }
            String expected = switch (lowerName) {
                case "x-auth-role-version" -> roleVersion;
                case "x-auth-department" -> department;
                case "x-user-id" -> actor;
                default -> null;
            };
            if ((lowerName.equals("x-auth-department") && expected == null)
                    || (expected != null && Collections.list(request.getHeaders(name)).stream()
                            .anyMatch(value -> !expected.equals(value)))) {
                throw new IllegalArgumentException("forwarded identity mismatch");
            }
        }
        // Controllers receive only claim-derived identity, so bypassing Gateway cannot grant a role.
        return new VerifiedUserRequest(request, actor, String.join(",", roles), roleVersion, department);
    }

    private HttpServletRequest verifyInternal(HttpServletRequest request) {
        if (serviceSecret == null || serviceSecret.isBlank() || serviceSecret.equals(userSecret)) {
            throw new IllegalArgumentException("separate internal signing key required");
        }
        if (request.getHeader(HttpHeaders.AUTHORIZATION) != null || request.getHeader("X-User-ID") != null
                || Collections.list(request.getHeaderNames()).stream()
                        .anyMatch(name -> name.toLowerCase(Locale.ROOT).startsWith("x-auth-"))) {
            throw new IllegalArgumentException("user credentials cannot authorize service mutation");
        }
        if (Collections.list(request.getHeaders("X-Service-Identity")).stream()
                .anyMatch(value -> !"closing".equals(value))) {
            throw new IllegalArgumentException("service identity mismatch");
        }
        String assertion = request.getHeader("X-Service-Assertion");
        if (Collections.list(request.getHeaders("X-Service-Assertion")).size() != 1) {
            throw new IllegalArgumentException("one service assertion required");
        }
        Claims claims = parse(assertion, serviceSecret, "closing-service", INTERNAL_AUDIENCE);
        // A short signed window bounds replay exposure without relying on caller-provided audit fields.
        if (claims.getExpiration().toInstant().isAfter(claims.getIssuedAt().toInstant().plusSeconds(300))) {
            throw new IllegalArgumentException("service assertion lifetime exceeds five minutes");
        }
        if (!"closing".equals(claims.getSubject())) {
            throw new IllegalArgumentException("closing principal required");
        }
        String actor = safeAuditActor(claims.get("actor", String.class));
        if (roles(claims.get("actorRoles")).stream().noneMatch(DELEGATED_ROLES::contains)) {
            throw new IllegalArgumentException("delegated actor role required");
        }
        Object periodClaim = claims.get("fiscalPeriodId");
        if (!(periodClaim instanceof Number periodNumber) || periodNumber.longValue() < 1
                || !Long.toString(periodNumber.longValue()).equals(periodNumber.toString())) {
            throw new IllegalArgumentException("signed fiscal period ID required");
        }
        long periodId = periodNumber.longValue();
        String closingStatus = claims.get("closingStatus", String.class);
        if (closingStatus == null || !Set.of("OPEN", "CLOSED", "PERMANENTLY_CLOSED").contains(closingStatus)) {
            throw new IllegalArgumentException("signed closing status required");
        }
        String targetPath = "/api/internal/fiscal-periods/" + periodId + "/closing-status";
        String requestPath = request.getRequestURI().substring(request.getContextPath().length());
        // A signed assertion is valid for one operation and one target, even while it remains unexpired.
        if (!"PUT".equals(request.getMethod()) || !"PUT".equals(claims.get("method", String.class))
                || !targetPath.equals(requestPath) || !targetPath.equals(claims.get("path", String.class))) {
            throw new IllegalArgumentException("service assertion target mismatch");
        }
        request.setAttribute(VERIFIED_SERVICE, "closing");
        request.setAttribute(VERIFIED_ACTOR, actor);
        request.setAttribute(VERIFIED_PERIOD_ID, periodId);
        request.setAttribute(VERIFIED_CLOSING_STATUS, closingStatus);
        return request;
    }

    private static Claims parse(String compact, String secret, String issuer, String audience) {
        if (compact == null || compact.isBlank() || secret == null
                || secret.getBytes(StandardCharsets.UTF_8).length < 32 || issuer == null || issuer.isBlank()
                || audience == null || audience.isBlank()) {
            throw new IllegalArgumentException("verification configuration or credential missing");
        }
        Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Jws<Claims> signed = Jwts.parserBuilder().setSigningKey(key).requireIssuer(issuer)
                .requireAudience(audience).build().parseClaimsJws(compact);
        if (!"HS256".equals(signed.getHeader().getAlgorithm())) {
            throw new IllegalArgumentException("HS256 assertion required");
        }
        Claims claims = signed.getBody();
        Date issuedAt = claims.getIssuedAt();
        Date expiresAt = claims.getExpiration();
        Instant now = Instant.now();
        if (issuedAt == null || expiresAt == null || !expiresAt.after(issuedAt)
                || issuedAt.toInstant().isAfter(now) || !expiresAt.toInstant().isAfter(now)) {
            throw new IllegalArgumentException("valid token time window required");
        }
        return claims;
    }

    private static String bearer(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")
                || Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION)).size() != 1) {
            throw new IllegalArgumentException("one bearer credential required");
        }
        return authorization.substring("Bearer ".length()).trim();
    }

    private static List<String> roles(Object value) {
        if (!(value instanceof List<?> raw) || raw.stream().anyMatch(role -> !(role instanceof String))) {
            throw new IllegalArgumentException("roles must be strings");
        }
        return raw.stream().map(String.class::cast).map(role -> safeHeaderValue(role, 80)).toList();
    }

    private static String safeHeaderValue(String value, int maxLength) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new IllegalArgumentException("unsafe identity claim");
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character < 0x21 || character > 0x7e || character == ',') {
                throw new IllegalArgumentException("unsafe identity claim");
            }
        }
        return value;
    }

    private static String safeAuditActor(String value) {
        if (value == null || value.length() > 42 || !value.matches("[A-Za-z0-9._@-]+")) {
            throw new IllegalArgumentException("unsafe delegated actor");
        }
        return value;
    }

    private static boolean isMutation(String method) {
        return HttpMethod.POST.matches(method) || HttpMethod.PUT.matches(method)
                || HttpMethod.PATCH.matches(method) || HttpMethod.DELETE.matches(method);
    }

    private static boolean hasAmbiguousPath(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        // MVC decodes percent escapes and removes matrix parameters after raw servlet URI inspection.
        return lower.indexOf(';') >= 0 || lower.indexOf('\\') >= 0 || lower.indexOf('%') >= 0
                || lower.contains("//")
                || Arrays.stream(lower.split("/", -1))
                        .anyMatch(segment -> segment.equals(".") || segment.equals(".."));
    }

    private static final class VerifiedUserRequest extends HttpServletRequestWrapper {
        private final Map<String, String> identity;

        private VerifiedUserRequest(HttpServletRequest request, String actor, String roles,
                String roleVersion, String department) {
            super(request);
            java.util.HashMap<String, String> values = new java.util.HashMap<>();
            values.put("X-Auth-User", actor);
            values.put("X-Auth-Roles", roles);
            values.put("X-Auth-Role-Version", roleVersion);
            values.put("X-User-ID", actor);
            if (department != null) {
                values.put("X-Auth-Department", department);
            }
            this.identity = Map.copyOf(values);
        }

        @Override
        public String getHeader(String name) {
            String verified = verifiedValue(name);
            return verified == null && isIdentityHeader(name) ? null
                    : verified == null ? super.getHeader(name) : verified;
        }

        @Override
        public java.util.Enumeration<String> getHeaders(String name) {
            String verified = verifiedValue(name);
            return verified == null && isIdentityHeader(name) ? Collections.emptyEnumeration()
                    : verified == null ? super.getHeaders(name) : Collections.enumeration(List.of(verified));
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>(identity.keySet());
            Collections.list(super.getHeaderNames()).stream()
                    .filter(name -> !isIdentityHeader(name))
                    .forEach(names::add);
            return Collections.enumeration(names);
        }

        private boolean isIdentityHeader(String name) {
            String lower = name.toLowerCase(Locale.ROOT);
            return lower.startsWith("x-auth-") || lower.equals("x-user-id");
        }

        private String verifiedValue(String name) {
            return identity.entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                    .map(Map.Entry::getValue)
                    .findFirst().orElse(null);
        }
    }
}
