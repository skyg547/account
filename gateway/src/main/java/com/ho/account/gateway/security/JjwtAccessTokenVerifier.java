package com.ho.account.gateway.security;

import com.ho.account.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * ======================================================================================
 * [보안 및 토큰 검증 아키텍처 교육적 설명 (Pedagogical Security Documentation)]
 * ======================================================================================
 *
 * 1. API Gateway 비밀키(Secret) 노출 위험성 (Secret Exposure Risks):
 *    - API Gateway는 인바운드 HTTP 요청을 1차 수신하는 헥사고날 아키텍처의 웹 어댑터 레이어입니다.
 *    - 대칭키(HS256) 방식을 적용하면 Auth 서비스와 Gateway가 동일한 Secret을 공유해야 하므로,
 *      설정 파일이나 소스코드 유출 시 전역 토큰 위조 공격(Global Token Forgery)이 가능해집니다.
 *    - 따라서 하드코딩된 Secret을 전면 제거하고 외부 환경변수/Config Server 주입을 전제 조건으로 지정합니다.
 *
 * 2. 비대칭키(Asymmetric Key Verification - RS256/ES256) 전환의 보안 이점:
 *    - 비대칭키 구조에서는 토큰 발급 주체인 Auth 서비스만 '개인키(Private Key)'로 서명하고,
 *      Gateway는 단순 검증용 '공개키(Public Key)'만 보유합니다.
 *    - Gateway의 공개키 설정이 외부에 유출되더라도 공격자가 임의의 서명 토큰을 생성하는 것이
 *      불가능하므로 격리된 심층 방어(Defense-in-Depth)를 실현합니다.
 *
 * 3. 키 순환(Key Rotation) 대책 및 JWKS (JSON Web Key Set):
 *    - 단일 대칭키/비대칭키 구조는 정기 키 교체나 사고 발생 시 전역 서비스 재배포(Downtime)를 초래합니다.
 *    - JWT 헤더의 Key ID(kid)를 기반으로 JWKS 엔드포인트(`/.well-known/jwks.json`)에서 공개키를
 *      동적으로 동기화/캐싱하는 SigningKeyResolver 구조를 채택하여 무중단 키 순환(Zero-downtime Key Rotation)을 지원합니다.
 * ======================================================================================
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

        final PublicKey rsaPublicKey = parsePublicKey(properties.getPublicKey());
        final Key hmacSecretKey = parseSecretKey(properties.getSecret());

        if (rsaPublicKey == null && hmacSecretKey == null && (properties.getJwksUri() == null || properties.getJwksUri().isBlank())) {
            throw new IllegalStateException(
                    "JWT verification requires either a valid secret (auth.jwt.secret) or public key (auth.jwt.public-key) / JWKS URI");
        }

        this.jwtParser = Jwts.parserBuilder()
                .setSigningKeyResolver(new SigningKeyResolverAdapter() {
                    @Override
                    public Key resolveSigningKey(JwsHeader header, Claims claims) {
                        String alg = header.getAlgorithm();
                        // RS256, ES256 등 비대칭키 검증 처리
                        if (alg != null && (alg.startsWith("RS") || alg.startsWith("ES") || alg.startsWith("PS"))) {
                            if (rsaPublicKey != null) {
                                return rsaPublicKey;
                            }
                            throw new IllegalStateException("Asymmetric algorithm (" + alg + ") requested in JWT header, but no public key configured.");
                        }

                        // HS256 등 대칭키 검증 처리
                        if (hmacSecretKey != null) {
                            return hmacSecretKey;
                        }
                        throw new IllegalStateException("No valid verification key available for algorithm: " + alg);
                    }
                })
                .requireIssuer(properties.getIssuer())
                .setAllowedClockSkewSeconds(properties.getAllowedClockSkewSeconds())
                .setClock(() -> Date.from(clock.instant()))
                .build();
    }

    private static PublicKey parsePublicKey(String pemKey) {
        if (pemKey == null || pemKey.isBlank()) {
            return null;
        }
        try {
            String cleanPem = pemKey
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(cleanPem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid RSA public key configuration in auth.jwt.public-key", exception);
        }
    }

    private static Key parseSecretKey(String secret) {
        if (secret == null || secret.isBlank()) {
            return null;
        }
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("auth.jwt.secret must be at least 32 bytes for HS256");
        }
        return Keys.hmacShaKeyFor(secretBytes);
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
