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

    @NotBlank
    private String baseUrl = "http://localhost:8084";

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
