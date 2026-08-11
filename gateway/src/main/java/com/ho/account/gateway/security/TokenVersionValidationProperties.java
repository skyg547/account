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
     * 기본값 'lb://auth-service'는 Spring Cloud LoadBalancer와 Eureka Service Discovery를 활용하는 식별자입니다.
     * 고정 IP(예: http://localhost:8084) 대신 'lb://서비스명' 스킴을 사용하면, Spring Cloud가 Discovery Registry에서
     * 가용 인스턴스 IP 목록을 조회하여 동적으로 트래픽을 부하 분산하고 동적 인스턴스 변경에 유연하게 대응합니다.
     */
    @NotBlank
    private String baseUrl = "lb://auth-service";

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
