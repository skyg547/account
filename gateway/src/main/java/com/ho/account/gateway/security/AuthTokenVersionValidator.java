package com.ho.account.gateway.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Auth의 `/api/auth/validate-token-version` API를 호출하는 Gateway 보안 어댑터입니다.
 *
 * <p>[교육적 주석: MSA 환경에서의 서비스 디스커버리 및 동적 로드밸런싱 통신]</p>
 * <p>1. 하드코딩된 IP/포트 호출의 문제점:
 * 기존에 `http://localhost:8084`와 같이 특정 IP/포트를 직접 지정하면, 동적으로 배포/확장(Scale-out)되는
 * MSA 환경에서 대상 서버가 재시작하거나 IP가 바뀔 때 매번 설정을 재배포해야 하고 트래픽 부하분산이 불가능합니다.</p>
 *
 * <p>2. 서비스 디스커버리와 Spring Cloud LoadBalancer 연동:
 * Spring Cloud LoadBalancer와 Eureka 서비스 디스커버리를 결합하여 `lb://auth-service` 형식을 사용할 경우,
 * Gateway는 Eureka Registry에서 등록된 auth-service 인스턴스 리스트(IP:Port)를 동적으로 조회합니다.
 * {@link LoadBalanced} WebClient는 가용 인스턴스들에 요청 트래픽을 부하 분산(Round-Robin 등)하여
 * 고가용성(HA)과 무중단 배포 및 유연한 확장성을 제공합니다.</p>
 *
 * <p>3. 역할 버전(roleVersion) 검증 및 캐싱:
 * 역할이 바뀌면 Auth의 roleVersion이 증가합니다. Gateway는 JWT 안의 버전과 Auth의
 * 현재 버전을 비교해서 오래된 토큰을 짧은 시간 안에 차단합니다. Auth 호출 실패나 빈 응답은 보안을
 * 위해 통과시키지 않되, 실제 권한 변경과 구분해 운영자가 장애 원인을 알 수 있게 합니다.</p>
 */
@Component
public class AuthTokenVersionValidator implements TokenVersionValidator {

    private static final Logger log = LoggerFactory.getLogger(AuthTokenVersionValidator.class);

    private final WebClient webClient;
    private final TokenVersionValidationProperties properties;
    private final Cache<TokenVersionKey, Boolean> validTokenCache;

    @Autowired
    public AuthTokenVersionValidator(@LoadBalanced WebClient.Builder webClientBuilder, TokenVersionValidationProperties properties) {
        this(webClientBuilder.baseUrl(properties.getBaseUrl()).build(), properties);
    }

    AuthTokenVersionValidator(WebClient webClient, TokenVersionValidationProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
        this.validTokenCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(properties.getCacheTtlSeconds()))
                .maximumSize(properties.getMaximumCacheSize())
                .build();
    }

    @Override
    public Mono<TokenVersionValidationResult> validate(String username, long roleVersion) {
        if (username == null || username.isBlank() || roleVersion < 1L) {
            return Mono.just(TokenVersionValidationResult.REJECTED);
        }
        if (!properties.isEnabled()) {
            return Mono.just(TokenVersionValidationResult.VALID);
        }

        TokenVersionKey key = new TokenVersionKey(username.trim(), roleVersion);
        if (validTokenCache.getIfPresent(key) != null) {
            return Mono.just(TokenVersionValidationResult.VALID);
        }

        // Auth 내부 검증 API에 mTLS 또는 서비스 자격 증명을 적용해 내부망 직접 호출도 인증한다.
        return webClient.post()
                .uri("/api/auth/validate-token-version")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TokenValidationRequest(key.username(), key.roleVersion()))
                .retrieve()
                .bodyToMono(TokenValidationResponse.class)
                .timeout(Duration.ofMillis(properties.getTimeoutMillis()))
                .map(response -> {
                    if (response.valid() == null) {
                        log.warn("AUTH_TOKEN_VERSION_VALIDATION_MALFORMED_RESPONSE roleVersion={}", key.roleVersion());
                        return TokenVersionValidationResult.UNAVAILABLE;
                    }
                    return response.valid()
                            ? TokenVersionValidationResult.VALID
                            : TokenVersionValidationResult.REJECTED;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("AUTH_TOKEN_VERSION_VALIDATION_EMPTY_RESPONSE roleVersion={}", key.roleVersion());
                    return Mono.just(TokenVersionValidationResult.UNAVAILABLE);
                }))
                .doOnNext(result -> {
                    if (result == TokenVersionValidationResult.VALID) {
                        // 다중 Gateway 노드에서는 Auth 역할 변경 이벤트로 positive cache를 즉시 무효화한다.
                        validTokenCache.put(key, true);
                    }
                })
                .onErrorResume(exception -> {
                    log.warn("AUTH_TOKEN_VERSION_VALIDATION_FAILED roleVersion={} cause={}",
                            key.roleVersion(), exception.getClass().getSimpleName());
                    return Mono.just(TokenVersionValidationResult.UNAVAILABLE);
                });
    }

    private record TokenVersionKey(String username, long roleVersion) {
    }

    private record TokenValidationRequest(String username, long roleVersion) {
    }

    private record TokenValidationResponse(Boolean valid, String reason) {
    }
}
