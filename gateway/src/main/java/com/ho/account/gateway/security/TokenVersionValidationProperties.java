package com.ho.account.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "auth.token-version-validation")
public class TokenVersionValidationProperties {

    private boolean enabled = true;
    private String baseUrl = "http://localhost:8084";
    private long cacheTtlSeconds = 30L;
    private long timeoutMillis = 500L;
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
