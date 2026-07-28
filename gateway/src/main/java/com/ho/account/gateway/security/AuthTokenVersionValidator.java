package com.ho.account.gateway.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Auth의 `/api/auth/validate-token-version` API를 호출하는 Gateway 보안 어댑터입니다.
 *
 * <p>초보자 설명: 역할이 바뀌면 Auth의 roleVersion이 증가합니다. Gateway는 JWT 안의 버전과 Auth의
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
    public AuthTokenVersionValidator(WebClient.Builder webClientBuilder, TokenVersionValidationProperties properties) {
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

        // @todo Auth 내부 검증 API에 mTLS 또는 서비스 자격 증명을 적용해 내부망 직접 호출도 인증한다.
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
                        // @todo 다중 Gateway 노드에서는 Auth 역할 변경 이벤트로 positive cache를 즉시 무효화한다.
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
