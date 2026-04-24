package com.ho.account.auth.core.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AuthModuleProperties.class)
public class AuthConfiguration {

    @Bean
    RestClient masterDataRestClient(AuthModuleProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getMasterData().getBaseUrl())
                .build();
    }
}
