package com.ho.account.configserver.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.configserver.ConfigServerApplication;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

/** Real servlet-container tests: URI overloads deliberately preserve percent encoding. */
@ExtendWith(OutputCaptureExtension.class)
class ConfigServerEncryptionEndpointSecurityIntegrationTest {
    private static final String TOKEN = UUID.randomUUID().toString();
    private static final String KEY = UUID.randomUUID().toString();

    @SpringBootTest(classes = ConfigServerApplication.class,
            webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
            properties = {
                    "spring.profiles.active=native",
                    "spring.cloud.config.server.native.search-locations=classpath:/config-repository",
                    "config-server.repository-probe.application=policy-test-service",
                    "config-server.repository-probe.profile=default",
                    "spring.cloud.config.server.health.repositories.master-data.name=policy-test-service",
                    "spring.cloud.config.server.health.repositories.master-data.profiles=default"
            })
    abstract static class HttpHarness {
        @Autowired TestRestTemplate http;
        @Autowired Environment environment;
        @LocalServerPort int port;

        @DynamicPropertySource
        static void key(DynamicPropertyRegistry registry) {
            registry.add("ENCRYPT_KEY", () -> KEY);
        }

        String root() {
            return "http://127.0.0.1:" + port
                    + environment.getProperty("server.servlet.context-path", "")
                    + environment.getProperty("spring.mvc.servlet.path", "");
        }

        String cryptoRoot() {
            return root() + environment.getProperty("spring.cloud.config.server.prefix", "");
        }

        ResponseEntity<String> post(String path, HttpHeaders headers, String body) {
            return http.exchange(URI.create(cryptoRoot() + path), HttpMethod.POST,
                    new HttpEntity<>(body, headers), String.class);
        }

        HttpHeaders authenticated(String header) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.set(header, HttpHeaders.AUTHORIZATION.equals(header) ? "Bearer " + TOKEN : TOKEN);
            return headers;
        }

        void safe(ResponseEntity<String> response, CapturedOutput output, String... values) {
            String body = response.getBody() == null ? "" : response.getBody();
            for (String value : values) {
                assertThat(body.contains(value)).as("error body must not echo test input").isFalse();
                assertThat(output.getAll().contains(value)).as("logs must not echo test input").isFalse();
            }
            assertThat(body.contains(TOKEN) || body.contains(KEY)).as("error body must not echo credentials").isFalse();
            assertThat(output.getAll().contains(TOKEN) || output.getAll().contains(KEY))
                    .as("logs must not echo credentials").isFalse();
        }

        @Test
        void healthReadinessAndConfigRemainPublic() {
            for (String path : List.of("/actuator/health", "/actuator/health/readiness")) {
                ResponseEntity<String> response = http.getForEntity(URI.create(root() + path), String.class);
                assertThat(response.getStatusCode().value()).isEqualTo(200);
                assertThat(response.getBody() != null && response.getBody().contains("\"status\":\"UP\""))
                        .as("health must remain UP").isTrue();
            }
            ResponseEntity<String> config = http.getForEntity(
                    URI.create(cryptoRoot() + "/policy-test-service/default"), String.class);
            assertThat(config.getStatusCode().value()).isEqualTo(200);
            assertThat(config.getBody() != null && config.getBody().contains("config-server-test"))
                    .as("native repository fixture must still be served").isTrue();
        }
    }

    @Nested
    @TestPropertySource(properties = "spring.cloud.config.server.encrypt.enabled=true")
    class Enabled extends HttpHarness {
        @DynamicPropertySource
        static void token(DynamicPropertyRegistry registry) {
            registry.add("config.crypto.endpoint.token", () -> TOKEN);
        }

        @ParameterizedTest
        @ValueSource(strings = {"/encrypt", "/decrypt", "/%65ncrypt", "/d%65crypt",
                "/encrypt;matrix=value", "/decrypt;matrix=value", "/encrypt/policy-test-service/default",
                "/decrypt/policy-test-service/default", "/%65ncrypt/policy-test-service/default",
                "/d%65crypt/policy-test-service/default", "/encrypt/", "/decrypt/"})
        void anonymousRequestsCannotReachCrypto(String path, CapturedOutput output) {
            String payload = UUID.randomUUID().toString();
            ResponseEntity<String> response = post(path, new HttpHeaders(), payload);
            assertThat(response.getStatusCode().value()).isEqualTo(401);
            safe(response, output, payload);
        }

        @ParameterizedTest
        @ValueSource(strings = {"X-Config-Token", "X-Config-Internal-Token", "Authorization"})
        void approvedHeadersAllowActualRoundTrip(String header, CapturedOutput output) {
            String payload = UUID.randomUUID().toString();
            for (int attempt = 0; attempt < 2; attempt++) {
                ResponseEntity<String> encrypted = post("/encrypt", authenticated(header), payload);
                assertThat(encrypted.getStatusCode().value()).isEqualTo(200);
                assertThat(encrypted.getBody() != null && !encrypted.getBody().isBlank()
                        && !encrypted.getBody().equals(payload)).as("encryption produces ciphertext").isTrue();
                ResponseEntity<String> decrypted = post("/decrypt", authenticated(header), encrypted.getBody());
                assertThat(decrypted.getStatusCode().value()).isEqualTo(200);
                assertThat(payload.equals(decrypted.getBody())).as("roundtrip preserves generated input").isTrue();
                assertThat(output.getAll().contains(payload) || output.getAll().contains(encrypted.getBody())
                        || output.getAll().contains(TOKEN) || output.getAll().contains(KEY))
                        .as("successful operations must not log sensitive input/output").isFalse();
            }
        }

        @ParameterizedTest
        @ValueSource(strings = {"/encrypt/policy-test-service/default", "/encrypt;matrix=value", "/%65ncrypt"})
        void authorizedAlternateRoutesUseRealController(String path) {
            ResponseEntity<String> response = post(path, authenticated("X-Config-Token"), UUID.randomUUID().toString());
            assertThat(response.getStatusCode().value()).isEqualTo(200);
        }

        @Test
        void wrongMalformedDuplicateAndConflictingCredentialsAreRejected(CapturedOutput output) {
            String wrong = UUID.randomUUID().toString();
            String payload = UUID.randomUUID().toString();
            for (String header : List.of("X-Config-Token", "X-Config-Internal-Token", "Authorization")) {
                HttpHeaders invalid = authenticated(header);
                invalid.set(header, HttpHeaders.AUTHORIZATION.equals(header) ? "Bearer " + wrong : wrong);
                assertDenied(invalid, payload, output, wrong);
                HttpHeaders duplicate = authenticated(header);
                duplicate.add(header, duplicate.getFirst(header));
                assertDenied(duplicate, payload, output, wrong);
            }
            HttpHeaders conflict = authenticated("X-Config-Token");
            conflict.set("X-Config-Internal-Token", wrong);
            assertDenied(conflict, payload, output, wrong);
            HttpHeaders malformed = authenticated("X-Config-Token");
            malformed.set(HttpHeaders.AUTHORIZATION, "Basic " + wrong);
            assertDenied(malformed, payload, output, wrong);
            for (String value : List.of("Bearer", "Bearer ", "Bearer  " + TOKEN, "Basic " + TOKEN)) {
                HttpHeaders bearer = new HttpHeaders();
                bearer.set(HttpHeaders.AUTHORIZATION, value);
                assertDenied(bearer, payload, output, wrong);
            }
        }

        private void assertDenied(HttpHeaders headers, String payload, CapturedOutput output, String wrong) {
            for (String path : List.of("/encrypt", "/decrypt")) {
                ResponseEntity<String> response = post(path, headers, payload);
                assertThat(response.getStatusCode().value()).isEqualTo(401);
                safe(response, output, payload, wrong);
            }
        }

        @Test
        void unauthorizedBodyAndContentTypeDoNotBypassAuthentication(CapturedOutput output) {
            for (String path : List.of("/encrypt", "/decrypt")) {
                for (MediaType type : List.of(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON)) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(type);
                    ResponseEntity<String> response = post(path, headers, "");
                    assertThat(response.getStatusCode().value()).isEqualTo(401);
                    safe(response, output);
                }
            }
        }

        @Test
        void invalidCiphertextAndEmptyBodyHaveSafeErrors(CapturedOutput output) {
            String invalid = UUID.randomUUID().toString();
            for (MediaType type : List.of(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON)) {
                HttpHeaders headers = authenticated("X-Config-Token");
                headers.setContentType(type);
                ResponseEntity<String> response = post("/decrypt", headers, invalid);
                assertThat(response.getStatusCode().is4xxClientError()).isTrue();
                safe(response, output, invalid);
            }
            ResponseEntity<String> empty = post("/encrypt", authenticated("X-Config-Token"), "");
            assertThat(empty.getStatusCode().is4xxClientError()).isTrue();
            safe(empty, output);
        }
    }

    @Nested
    @TestPropertySource(properties = {"spring.cloud.config.server.encrypt.enabled=true",
            "server.servlet.context-path=/context", "spring.mvc.servlet.path=/dispatcher",
            "spring.cloud.config.server.prefix=/configuration"})
    class Prefixed extends Enabled { }

    @Nested
    @TestPropertySource(properties = {"spring.cloud.config.server.encrypt.enabled=false"})
    class Disabled extends HttpHarness {
        @DynamicPropertySource
        static void token(DynamicPropertyRegistry registry) {
            registry.add("config.crypto.endpoint.token", () -> TOKEN);
        }

        @ParameterizedTest
        @ValueSource(strings = {"/encrypt", "/decrypt", "/%65ncrypt", "/d%65crypt",
                "/encrypt;matrix=value", "/decrypt;matrix=value",
                "/encrypt/policy-test-service/default", "/decrypt/policy-test-service/default"})
        void disabledRejectsAnonymousAndAuthenticated(String path, CapturedOutput output) {
            String payload = UUID.randomUUID().toString();
            for (HttpHeaders headers : List.of(new HttpHeaders(), authenticated("X-Config-Token"))) {
                ResponseEntity<String> response = post(path, headers, payload);
                assertThat(response.getStatusCode().value()).isEqualTo(404);
                safe(response, output, payload);
            }
        }
    }

    @Nested
    @TestPropertySource(properties = {"spring.cloud.config.server.encrypt.enabled=true",
            "config.crypto.endpoint.token=", "config-server.internal-crypto-token="})
    class MissingToken extends HttpHarness {
        @Test
        void invalidConfigurationFailsClosed(CapturedOutput output) {
            for (String path : List.of("/encrypt", "/decrypt", "/%65ncrypt", "/decrypt;matrix=value")) {
                String payload = UUID.randomUUID().toString();
                ResponseEntity<String> response = post(path, authenticated("X-Config-Token"), payload);
                assertThat(response.getStatusCode().value()).isEqualTo(403);
                safe(response, output, payload);
            }
        }
    }

    @Nested
    class BlankToken extends MissingToken {
        @DynamicPropertySource
        static void blank(DynamicPropertyRegistry registry) {
            registry.add("config.crypto.endpoint.token", () -> " \t ");
        }
    }

    @Nested
    class LeadingWhitespaceToken extends MissingToken {
        @DynamicPropertySource
        static void leadingWhitespace(DynamicPropertyRegistry registry) {
            registry.add("config.crypto.endpoint.token", () -> " " + TOKEN);
        }
    }

    @Nested
    class TrailingWhitespaceToken extends MissingToken {
        @DynamicPropertySource
        static void trailingWhitespace(DynamicPropertyRegistry registry) {
            registry.add("config.crypto.endpoint.token", () -> TOKEN + " ");
        }
    }

    @Nested
    class LegacyToken extends HttpHarness {
        @DynamicPropertySource
        static void legacy(DynamicPropertyRegistry registry) {
            registry.add("spring.cloud.config.server.encrypt.enabled", () -> "true");
            registry.add("config-server.internal-crypto-token", () -> TOKEN);
        }

        @Test
        void externalLegacyPropertyRemainsSupported() {
            ResponseEntity<String> response = post("/encrypt", authenticated("X-Config-Internal-Token"),
                    UUID.randomUUID().toString());
            assertThat(response.getStatusCode().value()).isEqualTo(200);
        }
    }
}
