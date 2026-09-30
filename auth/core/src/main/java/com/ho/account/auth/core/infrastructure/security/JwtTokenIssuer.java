package com.ho.account.auth.core.infrastructure.security;

import com.ho.account.auth.core.application.port.out.TokenIssuerPort;
import com.ho.account.auth.core.domain.model.RoleAssignment;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenIssuer implements TokenIssuerPort {

    private final AuthModuleProperties properties;
    private final Key signingKey;

    public JwtTokenIssuer(AuthModuleProperties properties) {
        this.properties = properties;
        byte[] secretBytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("auth.jwt.secret must be at least 32 bytes for HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

    @Override
    public IssuedToken issue(TokenSubject subject, Instant issuedAt) {
        if (issuedAt == null) {
            throw new IllegalArgumentException("issuedAt is required.");
        }
        long expirationSeconds = properties.getJwt().getExpirationSeconds();
        Instant expiresAt = issuedAt.plusSeconds(expirationSeconds);
        List<RoleAssignment> assignments = subject.effectiveRoleAssignments();
        // The claims must never reintroduce a scoped role that Auth cannot authorize downstream.
        if (assignments.stream().anyMatch(assignment -> !assignment.hasSupportedScope())) {
            throw new IllegalArgumentException("Unsupported role scope cannot be signed.");
        }
        List<String> roles = assignments.stream()
                .map(RoleAssignment::roleCode)
                .distinct()
                .toList();

        var builder = Jwts.builder()
                .setSubject(subject.username())
                .claim("roles", roles)
                .claim("roleVersion", subject.roleVersion())
                .claim("roleAssignments", assignments.stream()
                        .map(assignment -> Map.of(
                                "roleCode", assignment.roleCode(),
                                "dataScope", assignment.dataScope()))
                        .toList())
                .setIssuer(properties.getJwt().getIssuer())
                .setIssuedAt(Date.from(issuedAt))
                .setExpiration(Date.from(expiresAt));

        if (subject.departmentCode() != null && !subject.departmentCode().isBlank()) {
            builder.claim("departmentCode", subject.departmentCode());
        }

        String token = builder.signWith(signingKey, SignatureAlgorithm.HS256).compact();
        return new IssuedToken(token, expirationSeconds);
    }
}
