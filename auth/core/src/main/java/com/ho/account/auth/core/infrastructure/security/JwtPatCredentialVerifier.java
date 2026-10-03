package com.ho.account.auth.core.infrastructure.security;

import com.ho.account.auth.core.application.exception.PatAuthenticationException;
import com.ho.account.auth.core.application.model.PatActor;
import com.ho.account.auth.core.application.port.out.AuthUserQueryPort;
import com.ho.account.auth.core.application.port.out.PatCredentialVerifierPort;
import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JwtPatCredentialVerifier implements PatCredentialVerifierPort {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthUserQueryPort users;
    private final Clock clock;
    private final Key signingKey;
    private final String issuer;

    public JwtPatCredentialVerifier(AuthModuleProperties properties, AuthUserQueryPort users, Clock clock) {
        this.users = users;
        this.clock = clock;
        byte[] secretBytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("auth.jwt.secret must be at least 32 bytes for HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.issuer = properties.getJwt().getIssuer();
    }

    @Override
    public PatActor verify(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new PatAuthenticationException();
        }
        String credential = authorizationHeader.substring(BEARER_PREFIX.length());
        if (credential.isBlank() || credential.chars().anyMatch(Character::isWhitespace)) {
            throw new PatAuthenticationException();
        }

        Date issuedAt;
        Date expiresAt;
        String subject;
        long roleVersion;
        try {
            Jws<Claims> parsed = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .requireIssuer(issuer)
                    .setClock(() -> Date.from(clock.instant()))
                    .build()
                    .parseClaimsJws(credential);
            if (!SignatureAlgorithm.HS256.getValue().equals(parsed.getHeader().getAlgorithm())) {
                throw new PatAuthenticationException();
            }
            Claims claims = parsed.getBody();
            issuedAt = claims.getIssuedAt();
            expiresAt = claims.getExpiration();
            subject = claims.getSubject();
            roleVersion = roleVersion(claims.get("roleVersion"));
        } catch (JwtException | IllegalArgumentException | ClassCastException exception) {
            // Never include a submitted credential or parser detail in a public authentication error.
            throw new PatAuthenticationException();
        }

        Instant now = clock.instant();
        if (subject == null || subject.isBlank()
                || issuedAt == null || expiresAt == null
                || issuedAt.toInstant().isAfter(now)
                || !expiresAt.toInstant().isAfter(now)
                || !expiresAt.after(issuedAt)
                || roleVersion < 1) {
            throw new PatAuthenticationException();
        }

        AuthUser user = users.findByUsername(subject)
                .filter(candidate -> candidate.hasUsername(subject))
                .filter(AuthUser::isActive)
                .filter(candidate -> !candidate.isLocked())
                .filter(candidate -> candidate.getRoleVersion() == roleVersion)
                .orElseThrow(PatAuthenticationException::new);
        List<String> currentRoles = user.effectiveRolesAt(now);
        if (currentRoles.isEmpty()) {
            throw new PatAuthenticationException();
        }

        // JWT roles can be stale or caller-forged; only current approved AuthUser roles grant admin access.
        boolean systemAdmin = currentRoles.stream()
                .anyMatch(role -> role.equals("ROLE_SYSTEM_ADMIN") || role.equals("SYSTEM_ADMIN"));
        return new PatActor(user.getUsername(), systemAdmin);
    }

    private long roleVersion(Object claim) {
        if (!(claim instanceof Number number)) {
            return -1;
        }
        try {
            return new BigDecimal(number.toString()).longValueExact();
        } catch (NumberFormatException | ArithmeticException exception) {
            return -1;
        }
    }
}
