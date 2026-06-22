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
 * <p>🐣 역할이 바뀌면 Auth의 roleVersion이 증가합니다. Gateway는 JWT 안의 버전과 Auth의 현재 버전을
 * 비교해서 오래된 토큰을 짧은 시간 안에 차단합니다. Auth 호출 실패는 보안을 위해 거절(fail-closed)합니다.</p>
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
                .expireAfterWrite(Duration.ofSeconds(Math.max(1L, properties.getCacheTtlSeconds())))
                .maximumSize(Math.max(1L, properties.getMaximumCacheSize()))
                .build();
    }

    @Override
    public Mono<Boolean> validate(String username, long roleVersion) {
        if (!properties.isEnabled()) {
            return Mono.just(true);
        }
        if (username == null || username.isBlank() || roleVersion < 1L) {
            return Mono.just(false);
        }

        TokenVersionKey key = new TokenVersionKey(username.trim().toLowerCase(), roleVersion);
        Boolean cached = validTokenCache.getIfPresent(key);
        if (cached != null) {
            return Mono.just(cached);
        }

        return webClient.post()
                .uri("/api/auth/validate-token-version")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TokenValidationRequest(key.username(), key.roleVersion()))
                .retrieve()
                .bodyToMono(TokenValidationResponse.class)
                .timeout(Duration.ofMillis(Math.max(100L, properties.getTimeoutMillis())))
                .map(response -> response != null && response.valid())
                .doOnNext(valid -> {
                    if (valid) {
                        validTokenCache.put(key, true);
                    }
                })
                .onErrorResume(ex -> {
                    log.warn("AUTH_TOKEN_VERSION_VALIDATION_FAILED username={} roleVersion={} reason={}",
                            key.username(), key.roleVersion(), ex.getMessage());
                    return Mono.just(false);
                });
    }

    private record TokenVersionKey(String username, long roleVersion) {
    }

    private record TokenValidationRequest(String username, long roleVersion) {
    }

    private record TokenValidationResponse(boolean valid, String reason) {
    }
}
