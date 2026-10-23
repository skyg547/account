package com.ho.account.auth.core.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.auth.core.application.port.out.TokenIssuerPort;
import com.ho.account.auth.core.domain.model.RoleAssignment;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JwtTokenIssuerTest {

    private static final Instant ISSUED_AT = Instant.parse("2026-07-14T00:00:00Z");

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
        AuthModuleProperties properties = properties();
        JwtTokenIssuer issuer = new JwtTokenIssuer(properties);
        TokenIssuerPort.TokenSubject subject = new TokenIssuerPort.TokenSubject(
                "admin", "FIN", List.of(new RoleAssignment("ROLE_ADMIN", "FIN", null, null, true)), 1L);

        assertThatThrownBy(() -> issuer.issue(subject, Instant.parse("2026-07-14T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dataScope");
    }

    @Test
    @SuppressWarnings("unchecked")
    void capsPermanentAndTemporaryRoleTokenAtEarliestIncludedExpiry() {
        AuthModuleProperties properties = properties();
        JwtTokenIssuer issuer = new JwtTokenIssuer(properties);
        Instant adminExpiry = ISSUED_AT.plusSeconds(120);
        TokenIssuerPort.TokenSubject subject = new TokenIssuerPort.TokenSubject(
                "admin", "FIN", List.of(
                        RoleAssignment.approved("ROLE_USER"),
                        new RoleAssignment("ROLE_ADMIN", "GLOBAL", null, adminExpiry, true),
                        new RoleAssignment("ROLE_REVIEWER", "GLOBAL", null, ISSUED_AT.plusSeconds(240), true)),
                7L);

        TokenIssuerPort.IssuedToken issued = issuer.issue(subject, ISSUED_AT);

        // A surviving USER role must not extend a token that still carries ADMIN.
        assertThat(issued.expiresInSeconds()).isEqualTo(120L);
        Claims beforeExpiry = parse(issued.token(), properties, adminExpiry.minusSeconds(1));
        assertThat(beforeExpiry.getExpiration()).isEqualTo(Date.from(adminExpiry));
        assertThat(beforeExpiry.get("roles", List.class))
                .containsExactly("ROLE_USER", "ROLE_ADMIN", "ROLE_REVIEWER");
        // JJWT accepts equality with exp even with zero skew; Gateway still needs a strict boundary check.
        assertThat(parse(issued.token(), properties, adminExpiry).getExpiration())
                .isEqualTo(Date.from(adminExpiry));
        assertThatThrownBy(() -> parse(issued.token(), properties, adminExpiry.plusSeconds(1)))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void keepsConfiguredTtlWhenAllIncludedGrantsOutliveIt() {
        AuthModuleProperties properties = properties();
        JwtTokenIssuer issuer = new JwtTokenIssuer(properties);
        TokenIssuerPort.TokenSubject subject = new TokenIssuerPort.TokenSubject(
                "admin", "FIN", List.of(
                        RoleAssignment.approved("ROLE_USER"),
                        new RoleAssignment("ROLE_ADMIN", "GLOBAL", null, ISSUED_AT.plusSeconds(900), true)),
                7L);

        TokenIssuerPort.IssuedToken issued = issuer.issue(subject, ISSUED_AT);

        assertThat(issued.expiresInSeconds()).isEqualTo(600L);
        assertThat(parse(issued.token(), properties, ISSUED_AT.plusSeconds(599)).getExpiration())
                .isEqualTo(Date.from(ISSUED_AT.plusSeconds(600)));
        assertThatThrownBy(() -> parse(issued.token(), properties, ISSUED_AT.plusSeconds(601)))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void roundsSubsecondGrantExpiryDownToSignedJwtSecond() {
        AuthModuleProperties properties = properties();
        JwtTokenIssuer issuer = new JwtTokenIssuer(properties);
        Instant issuedAt = ISSUED_AT.plusMillis(250);
        Instant grantExpiry = ISSUED_AT.plusSeconds(10).plusMillis(750);
        TokenIssuerPort.TokenSubject subject = new TokenIssuerPort.TokenSubject(
                "admin", "FIN", List.of(
                        RoleAssignment.approved("ROLE_USER"),
                        new RoleAssignment("ROLE_ADMIN", "GLOBAL", null, grantExpiry, true)),
                7L);

        TokenIssuerPort.IssuedToken issued = issuer.issue(subject, issuedAt);

        // JJWT signs NumericDate to whole seconds; the response must not promise more usable time.
        assertThat(issued.expiresInSeconds()).isEqualTo(9L);
        assertThat(parse(issued.token(), properties, ISSUED_AT.plusSeconds(9)).getExpiration())
                .isEqualTo(Date.from(grantExpiry.truncatedTo(ChronoUnit.SECONDS)));
        assertThatThrownBy(() -> parse(issued.token(), properties, grantExpiry))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void refusesTokenWhoseNextSecondExpiryWouldReportZeroLifetime() {
        AuthModuleProperties properties = properties();
        JwtTokenIssuer issuer = new JwtTokenIssuer(properties);
        Instant issuedAt = ISSUED_AT.plusMillis(500);
        TokenIssuerPort.TokenSubject subject = new TokenIssuerPort.TokenSubject(
                "admin", "FIN", List.of(
                        new RoleAssignment("ROLE_ADMIN", "GLOBAL", null, ISSUED_AT.plusSeconds(1), true)),
                7L);

        // exp is one signed second later, but less than one real second remains at issuance.
        assertThatThrownBy(() -> issuer.issue(subject, issuedAt))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No issuable JWT lifetime");
    }

    private AuthModuleProperties properties() {
        AuthModuleProperties properties = new AuthModuleProperties();
        properties.getJwt().setSecret("test-secret-key-that-is-at-least-32-bytes-long!");
        properties.getJwt().setIssuer("auth-test");
        properties.getJwt().setExpirationSeconds(600L);
        return properties;
    }

    private Claims parse(String token, AuthModuleProperties properties, Instant parsedAt) {
        return Jwts.parserBuilder()
                .setClock(() -> Date.from(parsedAt))
                .setAllowedClockSkewSeconds(0)
                .setSigningKey(Keys.hmacShaKeyFor(
                        properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
