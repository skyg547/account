package com.ho.account.gateway.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * ======================================================================================
 * [보안 교육 및 키 관리 아키텍처 (Pedagogical Security Documentation)]
 * ======================================================================================
 *
 * 1. API Gateway에서 비밀키(Secret) 평문 하드코딩 노출 위험성 (Secret Exposure Risks):
 *    - API Gateway는 외부 인터넷 요청이 내부 마이크로서비스 생태계로 진입하는 최전방 관문(Edge Gateway)입니다.
 *    - 대칭키(HS256) 방식을 사용할 경우, Auth 서비스와 Gateway, 토큰 검증 백엔드가 동일한 비밀키를 공유합니다.
 *    - 소스코드(Git)나 설정 파일(application.yml)에 비밀키가 평문으로 하드코딩되면 유출 시 공격자가
 *      임의의 JWT 서명을 위조(Token Forgery)하여 마이크로서비스에 직통 접근하는 보안 사고가 발생합니다.
 *    - 따라서 평문 기본값 할당을 전면 제거하고 Config Server 또는 외부 환경변수(${AUTH_JWT_SECRET} / ${JWT_SECRET})를
 *      통해 런타임에 안전하게 주입받도록 구조화했습니다.
 *
 * 2. 비대칭키(Asymmetric Key Verification - RS256/ES256) 검증의 안보적 이점:
 *    - 비대칭키 서명 구조에서는 토큰 발급 주체인 Auth Service만 비밀키인 '개인키(Private Key)'를 보유하여 서명합니다.
 *    - API Gateway 및 각 마이크로서비스는 단순 서명 검증용 '공개키(Public Key)'만 보유합니다.
 *    - 공개키는 외부에 노출되어도 보안상 무방하므로, Gateway 설정이 유출되더라도 공격자의 토큰 위조가 불가능하여
 *      심층 방어(Defense-in-Depth)를 달성할 수 있습니다.
 *
 * 3. 키 순환(Key Rotation) 대책 및 JWKS (Key Rotation Strategies & JWKS):
 *    - 고정된 단일 비밀키/공개키는 유출 시 또는 정기 교체 시 모든 서비스의 재배포 및 시스템 중단(Downtime)을 유발합니다.
 *    - JWKS (JSON Web Key Set, RFC 7517) 규격을 적용하면 Auth Service가 `/.well-known/jwks.json` 엔드포인트로
 *      현재 사용 중인 공개키 목록과 Key ID(kid)를 제공하고, Gateway는 JWT 헤더의 `kid`를 참조하여 올바른 공개키를
 *      동적으로 조회/캐싱함으로써 무중단 키 순환(Zero-downtime Key Rotation)을 실현합니다.
 * ======================================================================================
 */
@Component
@Validated
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {

    /**
     * 대칭키(HMAC) 검증용 비밀키 (HS256).
     * 외부 주입 필수 (환경변수 AUTH_JWT_SECRET / JWT_SECRET 또는 Config Server).
     * 평문 기본값은 보안 정책에 따라 제거되었습니다.
     */
    private String secret;

    /**
     * 비대칭키(RSA/ECDSA) 검증용 공개키 (RS256/ES256).
     * PEM 형식 (-----BEGIN PUBLIC KEY----- ...) 또는 Base64 문자열.
     * 환경변수 AUTH_JWT_PUBLIC_KEY / JWT_PUBLIC_KEY 또는 Config Server에서 주입받을 수 있습니다.
     */
    private String publicKey;

    /**
     * 동적 키 순환(Key Rotation) 지원을 위한 JWKS (JSON Web Key Set) URI 엔드포인트.
     * 예: http://auth-service/oauth2/jwks 또는 http://auth-service/.well-known/jwks.json
     */
    private String jwksUri;

    @NotBlank
    private String issuer = "auth-service";

    @Min(0)
    @Max(300)
    private long allowedClockSkewSeconds = 30L;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public long getAllowedClockSkewSeconds() {
        return allowedClockSkewSeconds;
    }

    public void setAllowedClockSkewSeconds(long allowedClockSkewSeconds) {
        this.allowedClockSkewSeconds = allowedClockSkewSeconds;
    }
}
