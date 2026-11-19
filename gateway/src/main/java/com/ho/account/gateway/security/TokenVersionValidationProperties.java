package com.ho.account.gateway.security;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "auth.token-version-validation")
public class TokenVersionValidationProperties {

    private boolean enabled = true;

    /**
     * Auth 토큰 버전 검증 서비스를 호출하기 위한 Base URL입니다.
     *
     * <p>[교육적 주석: MSA 로드밸런싱 URL 구조]</p>
     * 기본값 'http://auth-service'의 host는 @LoadBalanced WebClient가 해석할 서비스 ID입니다.
     * HTTP 스킴은 인스턴스가 별도 스킴을 제공하지 않는 SimpleDiscovery에서도 유지됩니다.
     * Gateway route의 'lb://'는 라우팅 필터 전용이며, WebClient의 전송 프로토콜로 사용하지 않습니다.
     */
    @NotBlank
    private String baseUrl = "http://auth-service";

    @Min(1)
    @Max(300)
    private long cacheTtlSeconds = 30L;

    @Min(100)
    @Max(10_000)
    private long timeoutMillis = 500L;

    @Min(1)
    @Max(1_000_000)
    private long maximumCacheSize = 10_000L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public long getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(long cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public long getMaximumCacheSize() {
        return maximumCacheSize;
    }

    public void setMaximumCacheSize(long maximumCacheSize) {
        this.maximumCacheSize = maximumCacheSize;
    }
}
