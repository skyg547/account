package com.ho.account.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.gateway.config.JwtProperties;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Key;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

class JjwtAccessTokenVerifierTest {

    private static final String SECRET = ephemeralValue();
    private static final Instant NOW = Instant.parse("2026-07-20T00:00:00Z");

    private final JjwtAccessTokenVerifier verifier = new JjwtAccessTokenVerifier(
            properties(),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void verify_returnsHeaderSafePrincipalWhenEveryRequiredClaimIsValid() {
        String token = tokenBuilder()
                .claim("roles", List.of("ACCOUNT_ADMIN", "REPORT_READER"))
                .claim("roleVersion", 7L)
                .claim("departmentCode", "FINANCE")
                .compact();

        AuthenticatedPrincipal principal = verifier.verify(token);

        assertThat(principal.username()).isEqualTo("admin");
        assertThat(principal.roles()).containsExactly("ACCOUNT_ADMIN", "REPORT_READER");
        assertThat(principal.roleVersion()).isEqualTo(7L);
        assertThat(principal.departmentCode()).isEqualTo("FINANCE");
    }

    @Test
    void verify_verifiesTokenWithAsymmetricRsaPublicKey() {
        KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
        String publicKeyPem = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        JwtProperties rsaProps = new JwtProperties();
        rsaProps.setPublicKey(publicKeyPem);
        rsaProps.setIssuer("auth-service");

        JjwtAccessTokenVerifier rsaVerifier = new JjwtAccessTokenVerifier(
                rsaProps,
                Clock.fixed(NOW, ZoneOffset.UTC));

        String token = Jwts.builder()
                .setSubject("rsa_admin")
                .setIssuer("auth-service")
                .setIssuedAt(Date.from(NOW.minusSeconds(10)))
                .setExpiration(Date.from(NOW.plusSeconds(300)))
                .claim("roles", List.of("SYSTEM_ADMIN"))
                .claim("roleVersion", 1L)
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();

        AuthenticatedPrincipal principal = rsaVerifier.verify(token);

        assertThat(principal.username()).isEqualTo("rsa_admin");
        assertThat(principal.roles()).containsExactly("SYSTEM_ADMIN");
        assertThat(principal.roleVersion()).isEqualTo(1L);
    }

    @Test
    void constructor_throwsExceptionWhenNoKeyConfigured() {
        JwtProperties emptyProps = new JwtProperties();
        emptyProps.setSecret(null);

        assertThatThrownBy(() -> new JjwtAccessTokenVerifier(emptyProps, Clock.fixed(NOW, ZoneOffset.UTC)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT verification requires either a valid secret");
    }

    @Test
    void verify_rejectsMissingRoleVersionInsteadOfAssumingVersionOne() {
        String token = tokenBuilder()
                .claim("roles", List.of("ACCOUNT_ADMIN"))
                .compact();

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void verify_rejectsFractionalRoleVersion() {
        String token = tokenBuilder()
                .claim("roles", List.of("ACCOUNT_ADMIN"))
                .claim("roleVersion", 1.5D)
                .compact();

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void verify_rejectsMissingRoles() {
        String token = tokenBuilder()
                .claim("roleVersion", 1L)
                .compact();

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void verify_rejectsValuesThatCannotBeForwardedAsTrustedHeaders() {
        String token = tokenBuilder()
                .claim("roles", List.of("ADMIN,ROOT"))
                .claim("roleVersion", 1L)
                .compact();

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void verify_rejectsTokenWithoutExpiration() {
        String token = Jwts.builder()
                .setSubject("admin")
                .setIssuer("auth-service")
                .setIssuedAt(Date.from(NOW.minusSeconds(10)))
                .claim("roles", List.of("ACCOUNT_ADMIN"))
                .claim("roleVersion", 1L)
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    private JwtBuilder tokenBuilder() {
        return Jwts.builder()
                .setSubject("admin")
                .setIssuer("auth-service")
                .setIssuedAt(Date.from(NOW.minusSeconds(10)))
                .setExpiration(Date.from(NOW.plusSeconds(300)))
                .signWith(signingKey(), SignatureAlgorithm.HS256);
    }

    private Key signingKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private JwtProperties properties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setIssuer("auth-service");
        properties.setAllowedClockSkewSeconds(30L);
        return properties;
    }

    private static String ephemeralValue() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
