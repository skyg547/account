package com.ho.account.auth.core.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class DelegatingPasswordVerifierTest {

    private final DelegatingPasswordVerifier verifier = new DelegatingPasswordVerifier();
    private final BCryptPasswordEncoder bcryptEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("유효한 {bcrypt} 인코딩 패스워드는 일치 여부를 정상 검증한다")
    void matchesValidBcryptPassword() {
        String raw = "secret1234!";
        String encoded = "{bcrypt}" + bcryptEncoder.encode(raw);

        assertThat(verifier.matches(raw, encoded)).isTrue();
        assertThat(verifier.matches("wrongpassword", encoded)).isFalse();
    }

    @Test
    @DisplayName("{noop} 평문 패스워드는 거부되어 일치 검증에 실패한다")
    void rejectsNoopPassword() {
        assertThat(verifier.matches("1234", "{noop}1234")).isFalse();
        assertThat(verifier.matches("1234", "{NOOP}1234")).isFalse();
    }

    @Test
    @DisplayName("접두사가 없는 raw 패스워드는 거부된다")
    void rejectsRawPassword() {
        assertThat(verifier.matches("1234", "1234")).isFalse();
        assertThat(verifier.matches("secret", "secret")).isFalse();
    }

    @Test
    @DisplayName("빈 접두사 또는 잘못된 형식의 접두사는 거부된다")
    void rejectsMalformedOrEmptyPrefix() {
        assertThat(verifier.matches("1234", "{}1234")).isFalse();
        assertThat(verifier.matches("1234", "{ }1234")).isFalse();
        assertThat(verifier.matches("1234", "{bcrypt")).isFalse();
        assertThat(verifier.matches("1234", "{bcrypt}")).isFalse();
        assertThat(verifier.matches("1234", "{unknown}1234")).isFalse();
    }

    @Test
    @DisplayName("null 또는 빈 문자열 입력은 false를 반환한다")
    void returnsFalseForNullOrBlankInput() {
        assertThat(verifier.matches(null, "{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG")).isFalse();
        assertThat(verifier.matches("1234", null)).isFalse();
        assertThat(verifier.matches("", "{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG")).isFalse();
        assertThat(verifier.matches("1234", "")).isFalse();
        assertThat(verifier.matches("   ", "   ")).isFalse();
    }
}
