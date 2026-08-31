package com.ho.account.auth.core.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class DelegatingPasswordVerifierTest {

    private final DelegatingPasswordVerifier verifier = new DelegatingPasswordVerifier();

    private static String dynamicRawPassword() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String dynamicBcryptHash(String raw, int strength) {
        return new BCryptPasswordEncoder(strength).encode(raw);
    }

    @Test
    @DisplayName("정상: 동적으로 생성된 {bcrypt} 비밀번호는 올바른 평문과 일치하고 틀린 평문과 불일치한다")
    void matchesDynamicallyGeneratedBcryptPassword() {
        String raw = dynamicRawPassword();
        String wrong = dynamicRawPassword();
        String stored = "{bcrypt}" + dynamicBcryptHash(raw, 10);

        assertThat(verifier.matches(raw, stored)).isTrue();
        assertThat(verifier.matches(wrong, stored)).isFalse();
    }

    @Test
    @DisplayName("정상: 접두사 없는 순수 BCrypt 해시(60자)도 올바른 평문과 정상 일치한다")
    void matchesDynamicallyGeneratedRawBcryptHash() {
        String raw = dynamicRawPassword();
        String wrong = dynamicRawPassword();
        String stored = dynamicBcryptHash(raw, 10);

        assertThat(verifier.matches(raw, stored)).isTrue();
        assertThat(verifier.matches(wrong, stored)).isFalse();
    }

    @Test
    @DisplayName("경계값: 최소 cost factor (04)로 생성된 BCrypt 비밀번호가 정상 검증된다")
    void matchesBoundaryMinCostBcryptPassword() {
        String raw = dynamicRawPassword();
        String stored = "{bcrypt}" + dynamicBcryptHash(raw, 4);

        assertThat(verifier.matches(raw, stored)).isTrue();
        assertThat(verifier.matches("wrong-password", stored)).isFalse();
    }

    @Test
    @DisplayName("경계값: 최대 cost factor (31) 포맷 구조는 정책 검증기에서 유효한 구조로 통과한다")
    void handlesBoundaryMaxCostBcryptStructureSafely() {
        String raw = dynamicRawPassword();
        String baseHash = dynamicBcryptHash(raw, 4);
        String cost31Stored = "{bcrypt}$2a$31$" + baseHash.substring(7);

        assertThat(PasswordEncoderPolicy.isValid(cost31Stored)).isTrue();
        assertThat(PasswordEncoderPolicy.extractPayload(cost31Stored)).hasSize(60);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "plaintext",
            "1234",
            "adminPassword123!",
            "my_super_secret_password"
    })
    @DisplayName("실패: 평문 및 raw 패스워드는 거부되어 false를 반환한다")
    void rejectsRawPasswords(String rawStored) {
        assertThat(verifier.matches("1234", rawStored)).isFalse();
        assertThat(verifier.matches(rawStored, rawStored)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{noop}1234",
            "{noop}secret",
            "{NOOP}password",
            "{noop}"
    })
    @DisplayName("실패: {noop} 접두사 패스워드는 거부되어 false를 반환한다")
    void rejectsNoopPasswords(String noopStored) {
        assertThat(verifier.matches("1234", noopStored)).isFalse();
        assertThat(verifier.matches("secret", noopStored)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{unknown}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG",
            "{argon2}somehash",
            "{pbkdf2}somehash",
            "{sha256}somehash"
    })
    @DisplayName("실패: 알 수 없거나 지원되지 않는 접두사는 거부되어 false를 반환한다")
    void rejectsUnknownPrefixes(String unknownStored) {
        assertThat(verifier.matches("1234", unknownStored)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{ }",
            "{bcrypt",
            "bcrypt}",
            "{bcrypt}x",
            "{bcrypt}not-valid",
            "{bcrypt}$2a$10$short"
    })
    @DisplayName("실패: 비어있거나 불완전한 접두사 및 짧은 페이로드는 false를 반환한다")
    void rejectsMalformedPrefixAndShortPayload(String malformedStored) {
        assertThat(verifier.matches("1234", malformedStored)).isFalse();
    }

    @Test
    @DisplayName("실패: 허용 범위를 벗어난 cost factor (<04 또는 >31)는 거부된다")
    void rejectsOutOfRangeCostFactors() {
        String raw = dynamicRawPassword();
        String validHash = dynamicBcryptHash(raw, 4);
        String suffix53 = validHash.substring(7);

        String cost03Stored = "{bcrypt}$2a$03$" + suffix53;
        String cost32Stored = "{bcrypt}$2a$32$" + suffix53;

        assertThat(verifier.matches(raw, cost03Stored)).isFalse();
        assertThat(verifier.matches(raw, cost32Stored)).isFalse();
    }

    @Test
    @DisplayName("실패: 특수문자 등 malformed 문자가 포함된 페이로드는 거부된다")
    void rejectsMalformedCharactersInPayload() {
        String malformedPayload = "{bcrypt}$2a$10$" + "!".repeat(53);
        assertThat(verifier.matches("1234", malformedPayload)).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t\n  "})
    @DisplayName("실패: null 또는 공백 입력은 false를 반환한다")
    void returnsFalseForNullOrBlankInputs(String blank) {
        String validStored = "{bcrypt}" + dynamicBcryptHash("pass", 4);

        assertThat(verifier.matches(null, validStored)).isFalse();
        assertThat(verifier.matches("pass", null)).isFalse();
        assertThat(verifier.matches(blank, validStored)).isFalse();
        assertThat(verifier.matches("pass", blank)).isFalse();
        assertThat(verifier.matches(null, null)).isFalse();
    }

    @Test
    @DisplayName("반복성: 동일한 입력에 대해 다중 검증 시 결과가 일관되고 멱등적이다")
    void repeatableVerification() {
        String raw = dynamicRawPassword();
        String stored = "{bcrypt}" + dynamicBcryptHash(raw, 4);

        for (int i = 0; i < 30; i++) {
            assertThat(verifier.matches(raw, stored)).isTrue();
            assertThat(verifier.matches("wrong", stored)).isFalse();
            assertThat(verifier.matches(raw, "{noop}plain")).isFalse();
        }
    }
}
