package com.ho.account.configserver.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.configserver.ConfigServerApplication;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * ==============================================================================
 * Config Server Encryption Endpoint Security Integration Tests
 * ==============================================================================
 * 검증 항목:
 * 1. 암복호화 엔드포인트 비활성화(Disabled) 시 404 Not Found 반환 (Fail-Closed)
 * 2. 엔드포인트 활성화 상태에서 외부 토큰 미설정 시 403 Forbidden 반환 (Fail-Closed)
 * 3. 엔드포인트 활성화 + 토큰 설정 상태에서:
 *    - 익명(무인증) 요청 시 401 Unauthorized 거부
 *    - 변조/오류 토큰 요청 시 401 Unauthorized 거부
 *    - X-Config-Token 헤더 인증 시 200 OK 정상 처리
 *    - X-Config-Internal-Token 헤더 인증 시 200 OK 정상 처리
 *    - Authorization Bearer 헤더 인증 시 200 OK 정상 처리
 *    - Encrypt -> Decrypt 라운드트립 일관성
 *    - 동일 요청 반복 실행(Idempotence) 일관성
 *    - 공백 및 경계값 처리
 * 4. 보안 필터 활성화 상태에서도 Actuator(/actuator/health/readiness) 및
 *    일반 설정 조회(/{application}/{profile})는 100% 무회귀 200 OK 동작
 * ==============================================================================
 */
class ConfigServerEncryptionEndpointSecurityIntegrationTest {

    private static final String TEST_CRYPTO_TOKEN = "test-crypto-token-sec-99887766";
    private static final String TEST_SYMMETRIC_KEY = "test-symmetric-encryption-key-32bytes-len";

    @Nested
    @SpringBootTest(
            classes = ConfigServerApplication.class,
            webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
            properties = {
                    "spring.profiles.active=native",
                    "spring.cloud.config.server.native.search-locations=classpath:/config-repository",
                    "spring.cloud.config.server.encrypt.enabled=true",
                    "config.crypto.endpoint.token=" + TEST_CRYPTO_TOKEN,
                    "ENCRYPT_KEY=" + TEST_SYMMETRIC_KEY,
                    "config-server.repository-probe.application=policy-test-service",
                    "config-server.repository-probe.profile=default"
            })
    @DisplayName("암복호화 엔드포인트 활성화 및 토큰 구성 시 보안 정책 검증")
    class AuthenticatedEndpointSecurityTests {

        @Autowired
        private TestRestTemplate restTemplate;

        @Test
        @DisplayName("인증 헤더가 없는 익명 /encrypt 요청은 401 Unauthorized로 거부된다")
        void anonymousEncryptRequestIsRejectedWith401() {
            HttpEntity<String> request = new HttpEntity<>("sampleSecretData");
            ResponseEntity<String> response = restTemplate.postForEntity("/encrypt", request, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("인증 헤더가 없는 익명 /decrypt 요청은 401 Unauthorized로 거부된다")
        void anonymousDecryptRequestIsRejectedWith401() {
            HttpEntity<String> request = new HttpEntity<>("sampleCipherData");
            ResponseEntity<String> response = restTemplate.postForEntity("/decrypt", request, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("잘못된 X-Config-Token 헤더 요청은 401 Unauthorized로 거부된다")
        void invalidXConfigTokenIsRejectedWith401() {
            HttpHeaders headers = new HttpHeaders();
            headers.set(EncryptionEndpointSecurityFilter.CONFIG_TOKEN_HEADER, "invalid-forged-token");
            HttpEntity<String> request = new HttpEntity<>("sampleSecretData", headers);

            ResponseEntity<String> response = restTemplate.postForEntity("/encrypt", request, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("잘못된 Authorization Bearer 헤더 요청은 401 Unauthorized로 거부된다")
        void invalidBearerTokenIsRejectedWith401() {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer invalid-forged-token");
            HttpEntity<String> request = new HttpEntity<>("sampleSecretData", headers);

            ResponseEntity<String> response = restTemplate.postForEntity("/encrypt", request, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("올바른 X-Config-Token 헤더 요청은 /encrypt 및 /decrypt 성공(200 OK)")
        void validXConfigTokenAllowsEncryptAndDecrypt() {
            HttpHeaders headers = new HttpHeaders();
            headers.set(EncryptionEndpointSecurityFilter.CONFIG_TOKEN_HEADER, TEST_CRYPTO_TOKEN);
            HttpEntity<String> encryptRequest = new HttpEntity<>("myDatabasePassword123!", headers);

            ResponseEntity<String> encryptResponse =
                    restTemplate.postForEntity("/encrypt", encryptRequest, String.class);
            assertThat(encryptResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(encryptResponse.getBody()).isNotBlank();

            String encryptedCipher = encryptResponse.getBody();
            HttpEntity<String> decryptRequest = new HttpEntity<>(encryptedCipher, headers);
            ResponseEntity<String> decryptResponse =
                    restTemplate.postForEntity("/decrypt", decryptRequest, String.class);
            assertThat(decryptResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(decryptResponse.getBody()).isEqualTo("myDatabasePassword123!");
        }

        @Test
        @DisplayName("올바른 X-Config-Internal-Token 헤더 요청도 /encrypt 성공(200 OK)")
        void validInternalTokenHeaderAllowsEncrypt() {
            HttpHeaders headers = new HttpHeaders();
            headers.set(EncryptionEndpointSecurityFilter.INTERNAL_CONFIG_TOKEN_HEADER, TEST_CRYPTO_TOKEN);
            HttpEntity<String> request = new HttpEntity<>("internalSecretData", headers);

            ResponseEntity<String> response = restTemplate.postForEntity("/encrypt", request, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotBlank();
        }

        @Test
        @DisplayName("올바른 Authorization Bearer 헤더 요청도 /encrypt 성공(200 OK)")
        void validBearerTokenAllowsEncrypt() {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_CRYPTO_TOKEN);
            HttpEntity<String> request = new HttpEntity<>("bearerSecretData", headers);

            ResponseEntity<String> response = restTemplate.postForEntity("/encrypt", request, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotBlank();
        }

        @Test
        @DisplayName("동일한 평문에 대해 반복 요청 시 일관되게 암복호화 수행(재실행 정합성)")
        void repeatedRequestsAreConsistent() {
            HttpHeaders headers = new HttpHeaders();
            headers.set(EncryptionEndpointSecurityFilter.CONFIG_TOKEN_HEADER, TEST_CRYPTO_TOKEN);
            HttpEntity<String> request = new HttpEntity<>("repeatableSecret", headers);

            for (int i = 0; i < 3; i++) {
                ResponseEntity<String> encryptResponse =
                        restTemplate.postForEntity("/encrypt", request, String.class);
                assertThat(encryptResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(encryptResponse.getBody()).isNotBlank();

                HttpEntity<String> decryptRequest = new HttpEntity<>(encryptResponse.getBody(), headers);
                ResponseEntity<String> decryptResponse =
                        restTemplate.postForEntity("/decrypt", decryptRequest, String.class);
                assertThat(decryptResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(decryptResponse.getBody()).isEqualTo("repeatableSecret");
            }
        }

        @Test
        @DisplayName("하위 경로 및 슬래시 변형(/encrypt/, /decrypt/)에 대해서도 보안 필터가 동일하게 적용된다")
        void subpathAndSlashVariationsAreProtected() {
            HttpEntity<String> unauthenticatedRequest = new HttpEntity<>("secretData");
            ResponseEntity<String> slashEncryptResponse =
                    restTemplate.postForEntity("/encrypt/", unauthenticatedRequest, String.class);
            assertThat(slashEncryptResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

            ResponseEntity<String> slashDecryptResponse =
                    restTemplate.postForEntity("/decrypt/", unauthenticatedRequest, String.class);
            assertThat(slashDecryptResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("보안 필터가 활성화된 상태에서도 /actuator/health/readiness는 200 OK UP으로 정상 동작한다")
        void readinessEndpointIsUnaffectedByFilter() {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    "/actuator/health/readiness",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {});

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsEntry("status", "UP");
        }

        @Test
        @DisplayName("보안 필터가 활성화된 상태에서도 마이크로서비스 설정 조회(/{app}/{profile})는 200 OK로 정상 동작한다")
        void configurationEndpointIsUnaffectedByFilter() {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    "/policy-test-service/default",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {});

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("name")).isEqualTo("policy-test-service");
        }
    }

    @Nested
    @SpringBootTest(
            classes = ConfigServerApplication.class,
            webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
            properties = {
                    "spring.profiles.active=native",
                    "spring.cloud.config.server.native.search-locations=classpath:/config-repository",
                    "spring.cloud.config.server.encrypt.enabled=false",
                    "ENCRYPT_KEY=" + TEST_SYMMETRIC_KEY
            })
    @DisplayName("암복호화 엔드포인트 비활성화(Disabled) 시 보안 정책 검증")
    class DisabledEndpointSecurityTests {

        @Autowired
        private TestRestTemplate restTemplate;

        @Test
        @DisplayName("비활성화 상태에서는 유효한 토큰이 제공되어도 /encrypt는 404 Not Found를 반환한다")
        void disabledEncryptReturns404EvenWithToken() {
            HttpHeaders headers = new HttpHeaders();
            headers.set(EncryptionEndpointSecurityFilter.CONFIG_TOKEN_HEADER, TEST_CRYPTO_TOKEN);
            HttpEntity<String> request = new HttpEntity<>("secretData", headers);

            ResponseEntity<String> response = restTemplate.postForEntity("/encrypt", request, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("비활성화 상태에서는 /decrypt도 404 Not Found를 반환한다")
        void disabledDecryptReturns404() {
            HttpEntity<String> request = new HttpEntity<>("secretData");
            ResponseEntity<String> response = restTemplate.postForEntity("/decrypt", request, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @SpringBootTest(
            classes = ConfigServerApplication.class,
            webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
            properties = {
                    "spring.profiles.active=native",
                    "spring.cloud.config.server.native.search-locations=classpath:/config-repository",
                    "spring.cloud.config.server.encrypt.enabled=true",
                    "config.crypto.endpoint.token=",
                    "config-server.internal-crypto-token=",
                    "ENCRYPT_KEY=" + TEST_SYMMETRIC_KEY
            })
    @DisplayName("암복호화 엔드포인트 활성화되었으나 토큰 미설정(Fail-Closed) 시 보안 정책 검증")
    class MissingTokenEndpointSecurityTests {

        @Autowired
        private TestRestTemplate restTemplate;

        @Test
        @DisplayName("토큰이 설정되지 않은 상태에서는 403 Forbidden으로 Fail-Closed 차단된다")
        void missingTokenFailsClosedWith403() {
            HttpEntity<String> request = new HttpEntity<>("secretData");
            ResponseEntity<String> encryptResponse =
                    restTemplate.postForEntity("/encrypt", request, String.class);
            assertThat(encryptResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

            ResponseEntity<String> decryptResponse =
                    restTemplate.postForEntity("/decrypt", request, String.class);
            assertThat(decryptResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }
}
