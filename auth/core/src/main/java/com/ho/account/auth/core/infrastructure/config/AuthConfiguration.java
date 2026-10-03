package com.ho.account.auth.core.infrastructure.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AuthModuleProperties.class)
public class AuthConfiguration {

    @Bean
    Clock authClock() {
        return Clock.systemUTC();
    }

    @Bean
    RestClient masterDataRestClient(AuthModuleProperties properties) {
        var masterData = properties.getMasterData();
        // Pin the transport so classpath-dependent client selection cannot bypass the bounded waits.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(masterData.validatedConnectTimeoutMillis());
        requestFactory.setReadTimeout(masterData.validatedReadTimeoutMillis());
        return RestClient.builder()
                .baseUrl(masterData.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
