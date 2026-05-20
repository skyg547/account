package com.ho.account.auth.core.infrastructure.security;

import com.ho.account.auth.core.application.port.out.TokenIssuerPort;
import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
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
    public IssuedToken issue(AuthUser user) {
        long expirationSeconds = properties.getJwt().getExpirationSeconds();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationSeconds);

        var builder = Jwts.builder()
                .setSubject(user.getUsername())
                .claim("roles", user.getRoles())
                .claim("roleVersion", user.getRoleVersion())
                .claim("roleAssignments", user.getRoleAssignments().stream()
                        .filter(assignment -> assignment.isEffectiveAt(now))
                        .map(assignment -> Map.of(
                                "roleCode", assignment.roleCode(),
                                "dataScope", assignment.dataScope()))
                        .toList())
                .setIssuer(properties.getJwt().getIssuer())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt));

        if (user.getDepartmentCode() != null && !user.getDepartmentCode().isBlank()) {
            builder.claim("departmentCode", user.getDepartmentCode());
        }

        String token = builder.signWith(signingKey, SignatureAlgorithm.HS256).compact();

        return new IssuedToken(token, expirationSeconds);
    }
}
