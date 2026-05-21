package com.ho.account.audit.infrastructure.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "governance.integrations.auth")
public class AuthIntegrationProperties {

    private String baseUrl = "http://localhost:8081";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
