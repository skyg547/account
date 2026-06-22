package com.ho.account.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class AuthTokenVersionValidatorTest {

    @Test
    void validate_returnsTrueAndCachesValidResponse() {
        AtomicInteger calls = new AtomicInteger();
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    calls.incrementAndGet();
                    assertThat(request.url().getPath()).isEqualTo("/api/auth/validate-token-version");
                    return Mono.just(ClientResponse.create(HttpStatus.OK)
                            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .body("{\"valid\":true,\"reason\":\"OK\"}")
                            .build());
                })
                .build();

        AuthTokenVersionValidator validator = new AuthTokenVersionValidator(webClient, properties());

        assertThat(validator.validate("Admin", 3L).block()).isTrue();
        assertThat(validator.validate("admin", 3L).block()).isTrue();
        assertThat(calls).hasValue(1);
    }

    @Test
    void validate_failsClosedWhenAuthCallFails() {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.error(new IllegalStateException("auth unavailable")))
                .build();

        AuthTokenVersionValidator validator = new AuthTokenVersionValidator(webClient, properties());

        assertThat(validator.validate("admin", 3L).block()).isFalse();
    }

    @Test
    void validate_canBeDisabledForIsolatedLocalGatewaySmoke() {
        TokenVersionValidationProperties properties = properties();
        properties.setEnabled(false);
        AuthTokenVersionValidator validator = new AuthTokenVersionValidator(WebClient.create(), properties);

        assertThat(validator.validate("admin", 3L).block()).isTrue();
    }

    private TokenVersionValidationProperties properties() {
        TokenVersionValidationProperties properties = new TokenVersionValidationProperties();
        properties.setCacheTtlSeconds(30L);
        properties.setTimeoutMillis(500L);
        properties.setMaximumCacheSize(100L);
        return properties;
    }
}
