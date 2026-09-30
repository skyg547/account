package com.ho.account.auth.core.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.auth.core.application.port.out.TokenIssuerPort;
import com.ho.account.auth.core.domain.model.RoleAssignment;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JwtTokenIssuerTest {

    @Test
    @SuppressWarnings("unchecked")
    void issuesClaimsFromProvidedRoleSnapshotAndIssuedAt() {
        AuthModuleProperties properties = new AuthModuleProperties();
        properties.getJwt().setSecret("test-secret-key-that-is-at-least-32-bytes-long!");
        properties.getJwt().setIssuer("auth-test");
        properties.getJwt().setExpirationSeconds(600L);
        JwtTokenIssuer issuer = new JwtTokenIssuer(properties);
        Instant issuedAt = Instant.parse("2026-07-14T00:00:00Z");
        TokenIssuerPort.TokenSubject subject = new TokenIssuerPort.TokenSubject(
                "admin",
                "FIN",
                List.of(new RoleAssignment("ROLE_ADMIN", "GLOBAL", null, null, true)),
                7L);

        TokenIssuerPort.IssuedToken issued = issuer.issue(subject, issuedAt);

        Claims claims = Jwts.parserBuilder()
                .setClock(() -> Date.from(issuedAt.plusSeconds(1)))
                .setSigningKey(Keys.hmacShaKeyFor(
                        properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(issued.token())
                .getBody();
        assertThat(claims.getSubject()).isEqualTo("admin");
        assertThat(claims.getIssuer()).isEqualTo("auth-test");
        assertThat(claims.getAudience()).isEqualTo("account-api");
        assertThat(claims.getIssuedAt()).isEqualTo(Date.from(issuedAt));
        assertThat(claims.getExpiration()).isEqualTo(Date.from(issuedAt.plusSeconds(600L)));
        assertThat(claims.get("roles", List.class)).containsExactly("ROLE_ADMIN");
        assertThat(claims.get("roleVersion", Number.class).longValue()).isEqualTo(7L);
        assertThat(claims.get("departmentCode", String.class)).isEqualTo("FIN");
        assertThat(claims.get("roleAssignments", List.class))
                .containsExactly(Map.of("roleCode", "ROLE_ADMIN", "dataScope", "GLOBAL"));
        assertThat(issued.expiresInSeconds()).isEqualTo(600L);
    }

    @Test
    void directIssuerRefusesLegacyScopedAssignment() {
        AuthModuleProperties properties = new AuthModuleProperties();
        properties.getJwt().setSecret("test-secret-key-that-is-at-least-32-bytes-long!");
        JwtTokenIssuer issuer = new JwtTokenIssuer(properties);
        TokenIssuerPort.TokenSubject subject = new TokenIssuerPort.TokenSubject(
                "admin", "FIN", List.of(new RoleAssignment("ROLE_ADMIN", "FIN", null, null, true)), 1L);

        assertThatThrownBy(() -> issuer.issue(subject, Instant.parse("2026-07-14T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dataScope");
    }
}
