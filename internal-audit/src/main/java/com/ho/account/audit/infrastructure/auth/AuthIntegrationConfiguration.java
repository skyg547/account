package com.ho.account.audit.infrastructure.auth;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AuthIntegrationProperties.class)
public class AuthIntegrationConfiguration {

    @Bean
    @Qualifier("authRestClient")
    RestClient authRestClient(AuthIntegrationProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }
}
