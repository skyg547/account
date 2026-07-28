package com.ho.account.gateway.security;

import com.ho.account.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JJWT로 HS256 서명과 issuer를 검증하고 필수 claim을 내부 신원으로 변환하는 어댑터입니다.
 *
 * <p>초보자 설명: 서명이 맞는지만 확인하면 충분하지 않습니다. 만료 시각, 발급 시각, 사용자,
 * 역할, roleVersion이 모두 있어야 뒤쪽 서비스가 같은 권한 스냅샷을 신뢰할 수 있습니다.</p>
 */
@Component
public class JjwtAccessTokenVerifier implements AccessTokenVerifier {

    private final JwtParser jwtParser;
    private final JwtProperties properties;
    private final Clock clock;

    @Autowired
    public JjwtAccessTokenVerifier(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    JjwtAccessTokenVerifier(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        byte[] secretBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("auth.jwt.secret must be at least 32 bytes for HS256");
        }
        Key signingKey = Keys.hmacShaKeyFor(secretBytes);

        //  Auth와 Gateway의 공유 HS256 키를 JWKS 기반 비대칭 키/키 회전 계약으로 전환한다.
        this.jwtParser = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .requireIssuer(properties.getIssuer())
                .setAllowedClockSkewSeconds(properties.getAllowedClockSkewSeconds())
                .setClock(() -> Date.from(clock.instant()))
                .build();
    }

    @Override
    public AuthenticatedPrincipal verify(String token) {
        try {
            Claims claims = jwtParser.parseClaimsJws(token).getBody();
            validateTimeClaims(claims);
            return new AuthenticatedPrincipal(
                    claims.getSubject(),
                    resolveRoles(claims),
                    resolveRoleVersion(claims),
                    resolveDepartmentCode(claims));
        } catch (RuntimeException exception) {
            throw new InvalidAccessTokenException("access token is invalid", exception);
        }
    }

    private void validateTimeClaims(Claims claims) {
        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();
        if (issuedAt == null || expiration == null) {
            throw new IllegalArgumentException("issuedAt and expiration are required");
        }
        if (!expiration.after(issuedAt)) {
            throw new IllegalArgumentException("expiration must be after issuedAt");
        }

        Instant latestAcceptedIssuedAt = clock.instant().plusSeconds(properties.getAllowedClockSkewSeconds());
        if (issuedAt.toInstant().isAfter(latestAcceptedIssuedAt)) {
            throw new IllegalArgumentException("issuedAt is in the future");
        }
    }

    private List<String> resolveRoles(Claims claims) {
        Object rolesClaim = claims.get("roles");
        if (!(rolesClaim instanceof List<?> values)) {
            throw new IllegalArgumentException("roles claim must be a list");
        }
        return values.stream()
                .map(value -> {
                    if (!(value instanceof String role)) {
                        throw new IllegalArgumentException("every roles claim value must be a string");
                    }
                    return role;
                })
                .toList();
    }

    private long resolveRoleVersion(Claims claims) {
        Object roleVersionClaim = claims.get("roleVersion");
        if (!(roleVersionClaim instanceof Number number)) {
            throw new IllegalArgumentException("roleVersion claim must be a number");
        }
        try {
            return new BigDecimal(number.toString()).longValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException("roleVersion claim must be an integer", exception);
        }
    }

    private String resolveDepartmentCode(Claims claims) {
        Object departmentCodeClaim = claims.get("departmentCode");
        if (departmentCodeClaim == null) {
            return null;
        }
        if (!(departmentCodeClaim instanceof String departmentCode)) {
            throw new IllegalArgumentException("departmentCode claim must be a string");
        }
        return departmentCode;
    }
}
