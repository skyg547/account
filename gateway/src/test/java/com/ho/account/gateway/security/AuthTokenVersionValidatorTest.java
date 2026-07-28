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
    void validate_returnsValidAndCachesOnlyValidResponse() {
        AtomicInteger calls = new AtomicInteger();
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    calls.incrementAndGet();
                    assertThat(request.url().getPath()).isEqualTo("/api/auth/validate-token-version");
                    return jsonResponse("{\"valid\":true,\"reason\":\"OK\"}");
                })
                .build();

        AuthTokenVersionValidator validator = new AuthTokenVersionValidator(webClient, properties());

        assertThat(validator.validate("Admin", 3L).block()).isEqualTo(TokenVersionValidationResult.VALID);
        assertThat(validator.validate("Admin", 3L).block()).isEqualTo(TokenVersionValidationResult.VALID);
        assertThat(calls).hasValue(1);
    }

    @Test
    void validate_doesNotMergeCaseSensitiveCanonicalUsernamesInCache() {
        AtomicInteger calls = new AtomicInteger();
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    calls.incrementAndGet();
                    return jsonResponse("{\"valid\":true,\"reason\":\"OK\"}");
                })
                .build();
        AuthTokenVersionValidator validator = new AuthTokenVersionValidator(webClient, properties());

        assertThat(validator.validate("Admin", 3L).block()).isEqualTo(TokenVersionValidationResult.VALID);
        assertThat(validator.validate("admin", 3L).block()).isEqualTo(TokenVersionValidationResult.VALID);
        assertThat(calls).hasValue(2);
    }

    @Test
    void validate_returnsRejectedWhenAuthRejectsCurrentSnapshot() {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> jsonResponse("{\"valid\":false,\"reason\":\"ROLE_VERSION_MISMATCH\"}"))
                .build();

        AuthTokenVersionValidator validator = new AuthTokenVersionValidator(webClient, properties());

        assertThat(validator.validate("admin", 3L).block()).isEqualTo(TokenVersionValidationResult.REJECTED);
    }

    @Test
    void validate_returnsUnavailableWhenAuthCallFails() {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.error(new IllegalStateException("auth unavailable")))
                .build();

        AuthTokenVersionValidator validator = new AuthTokenVersionValidator(webClient, properties());

        assertThat(validator.validate("admin", 3L).block()).isEqualTo(TokenVersionValidationResult.UNAVAILABLE);
    }

    @Test
    void validate_returnsUnavailableWhenAuthResponseOmitsValidField() {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> jsonResponse("{}"))
                .build();

        AuthTokenVersionValidator validator = new AuthTokenVersionValidator(webClient, properties());

        assertThat(validator.validate("admin", 3L).block()).isEqualTo(TokenVersionValidationResult.UNAVAILABLE);
    }
    @Test
    void validate_returnsUnavailableWhenAuthResponseBodyIsEmpty() {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK).build()))
                .build();

        AuthTokenVersionValidator validator = new AuthTokenVersionValidator(webClient, properties());

        assertThat(validator.validate("admin", 3L).block()).isEqualTo(TokenVersionValidationResult.UNAVAILABLE);
    }

    @Test
    void validate_canBeDisabledForIsolatedLocalGatewaySmoke() {
        TokenVersionValidationProperties properties = properties();
        properties.setEnabled(false);
        AuthTokenVersionValidator validator = new AuthTokenVersionValidator(WebClient.create(), properties);

        assertThat(validator.validate("admin", 3L).block()).isEqualTo(TokenVersionValidationResult.VALID);
        assertThat(validator.validate(" ", 0L).block()).isEqualTo(TokenVersionValidationResult.REJECTED);
    }

    private Mono<ClientResponse> jsonResponse(String body) {
        return Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build());
    }

    private TokenVersionValidationProperties properties() {
        TokenVersionValidationProperties properties = new TokenVersionValidationProperties();
        properties.setCacheTtlSeconds(30L);
        properties.setTimeoutMillis(500L);
        properties.setMaximumCacheSize(100L);
        return properties;
    }
}
