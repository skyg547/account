package com.ho.account.configserver.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.configserver.ConfigServerApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        classes = ConfigServerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.profiles.active=native",
                "spring.cloud.config.server.native.search-locations=classpath:/config-repository",
                "spring.cloud.config.server.encrypt.enabled=true",
                "config-server.internal-crypto-token=test-crypto-internal-token-12345",
                "ENCRYPT_KEY=test-symmetric-key-for-unit-test-32bytes"
        })
class ConfigServerEncryptionEndpointSecurityIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("인증 헤더가 없는 익명 요청은 401 Unauthorized로 거부된다")
    void anonymousRequestIsRejectedWithUnauthorized() {
        HttpEntity<String> request = new HttpEntity<>("secretData");
        ResponseEntity<String> response = restTemplate.postForEntity("/encrypt", request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("유효하지 않거나 변조된 토큰은 401 Unauthorized로 거부된다")
    void invalidTokenIsRejectedWithUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Config-Internal-Token", "invalid-forged-token");
        HttpEntity<String> request = new HttpEntity<>("secretData", headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/encrypt", request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("정상적인 내부 인증 토큰을 가진 요청은 암복호화 엔드포인트 접근이 허용된다")
    void validInternalTokenIsAllowed() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Config-Internal-Token", "test-crypto-internal-token-12345");
        HttpEntity<String> request = new HttpEntity<>("secretData", headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/encrypt", request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotBlank();
    }
}