package com.ho.account.auth.core.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordEncoderPolicyTest {

    private static final BCryptPasswordEncoder DEFAULT_BCRYPT = new BCryptPasswordEncoder();

    private static String dynamicRawPassword() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String dynamicBcryptHash(int strength) {
        return new BCryptPasswordEncoder(strength).encode(dynamicRawPassword());
    }

    @Test
    @DisplayName("정상: 동적으로 생성된 유효한 {bcrypt} 비밀번호는 성공적으로 검증된다")
    void validatesDynamicallyGeneratedBcryptWithPrefix() {
        String dynamicHash = dynamicBcryptHash(10);
        String stored = "{bcrypt}" + dynamicHash;

        assertThat(PasswordEncoderPolicy.isValid(stored)).isTrue();
        PasswordEncoderPolicy.validate(stored);
        assertThat(PasswordEncoderPolicy.extractPayload(stored)).isEqualTo(dynamicHash);
        assertThat(PasswordEncoderPolicy.formatWithPrefix(dynamicHash)).isEqualTo(stored);
    }

    @Test
    @DisplayName("정상: 접두사 없는 순수 BCrypt 해시(60자)도 성공적으로 검증된다")
    void validatesDynamicallyGeneratedRawBcryptPayload() {
        String dynamicHash = dynamicBcryptHash(10);

        assertThat(PasswordEncoderPolicy.isValid(dynamicHash)).isTrue();
        PasswordEncoderPolicy.validate(dynamicHash);
        assertThat(PasswordEncoderPolicy.extractPayload(dynamicHash)).isEqualTo(dynamicHash);
        assertThat(PasswordEncoderPolicy.formatWithPrefix(dynamicHash)).isEqualTo("{bcrypt}" + dynamicHash);
    }

    @Test
    @DisplayName("정상: 지원되는 모든 BCrypt 버전($2a$, $2b$, $2y$)을 허용한다")
    void acceptsSupportedBcryptVersions() {
        String baseHash = dynamicBcryptHash(10);
        String suffix56 = baseHash.substring(4); // 56 chars: "10$..."

        String version2a = "$2a$" + suffix56;
        String version2b = "$2b$" + suffix56;
        String version2y = "$2y$" + suffix56;

        assertThat(PasswordEncoderPolicy.isValid("{bcrypt}" + version2a)).isTrue();
        assertThat(PasswordEncoderPolicy.isValid("{bcrypt}" + version2b)).isTrue();
        assertThat(PasswordEncoderPolicy.isValid("{bcrypt}" + version2y)).isTrue();
    }

    @Test
    @DisplayName("경계값: 최소 cost factor (04)와 최대 cost factor (31)를 정상 허용한다")
    void acceptsBoundaryCostFactors() {
        String minCostHash = dynamicBcryptHash(4);
        assertThat(PasswordEncoderPolicy.isValid("{bcrypt}" + minCostHash)).isTrue();
        assertThat(PasswordEncoderPolicy.isValid(minCostHash)).isTrue();

        // 31은 연산 비용이 매우 높으므로 규격 포맷($2a$31$ + 53자 radix64)으로 검증
        String rawPayload = dynamicBcryptHash(4);
        String maxCostHash = "$2a$31$" + rawPayload.substring(7);
        assertThat(maxCostHash).hasSize(60);
        assertThat(PasswordEncoderPolicy.isValid("{bcrypt}" + maxCostHash)).isTrue();
        assertThat(PasswordEncoderPolicy.isValid(maxCostHash)).isTrue();
    }

    @Test
    @DisplayName("경계값: cost factor가 04 미만(03)이거나 31 초과(32, 99)인 경우 거부된다")
    void rejectsOutOfRangeCostFactors() {
        String validHash = dynamicBcryptHash(4);
        String suffix53 = validHash.substring(7); // 53 chars radix64

        String cost03 = "$2a$03$" + suffix53;
        String cost00 = "$2a$00$" + suffix53;
        String cost32 = "$2a$32$" + suffix53;
        String cost99 = "$2a$99$" + suffix53;

        assertThat(PasswordEncoderPolicy.isValid("{bcrypt}" + cost03)).isFalse();
        assertThat(PasswordEncoderPolicy.isValid("{bcrypt}" + cost00)).isFalse();
        assertThat(PasswordEncoderPolicy.isValid("{bcrypt}" + cost32)).isFalse();
        assertThat(PasswordEncoderPolicy.isValid("{bcrypt}" + cost99)).isFalse();

        assertThatThrownBy(() -> PasswordEncoderPolicy.validate("{bcrypt}" + cost03))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cost factor must be between 4 and 31");

        assertThatThrownBy(() -> PasswordEncoderPolicy.validate("{bcrypt}" + cost32))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cost factor must be between 4 and 31");
    }

    @Test
    @DisplayName("경계값: BCrypt 페이로드가 정확히 60자가 아닌 경우(59자, 61자) 거부된다")
    void rejectsPayloadsWithInvalidLength() {
        String validHash = dynamicBcryptHash(4);
        String short59 = validHash.substring(0, 59);
        String long61 = validHash + "A";

        assertThat(short59).hasSize(59);
        assertThat(long61).hasSize(61);

        assertThat(PasswordEncoderPolicy.isValid("{bcrypt}" + short59)).isFalse();
        assertThat(PasswordEncoderPolicy.isValid("{bcrypt}" + long61)).isFalse();

        assertThatThrownBy(() -> PasswordEncoderPolicy.validate("{bcrypt}" + short59))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be exactly 60 characters, but was 59");

        assertThatThrownBy(() -> PasswordEncoderPolicy.validate("{bcrypt}" + long61))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be exactly 60 characters, but was 61");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t\n  "})
    @DisplayName("실패: null 또는 공백 문자열은 거부된다")
    void rejectsNullOrBlank(String input) {
        assertThat(PasswordEncoderPolicy.isValid(input)).isFalse();
        assertThatThrownBy(() -> PasswordEncoderPolicy.validate(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null or blank");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "plaintext",
            "1234",
            "adminPassword123!",
            "my_super_secret_password"
    })
    @DisplayName("실패: 평문 및 접두사 없는 raw 텍스트는 거부된다")
    void rejectsRawPlaintext(String rawText) {
        assertThat(PasswordEncoderPolicy.isValid(rawText)).isFalse();
        assertThatThrownBy(() -> PasswordEncoderPolicy.validate(rawText))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{noop}1234",
            "{noop}secret",
            "{NOOP}password",
            "{noop}"
    })
    @DisplayName("실패: {noop} 접두사는 보안 정책상 금지된다")
    void rejectsNoopPrefix(String noopPassword) {
        assertThat(PasswordEncoderPolicy.isValid(noopPassword)).isFalse();
        assertThatThrownBy(() -> PasswordEncoderPolicy.validate(noopPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("forbidden '{noop}' prefix");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{unknown}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG",
            "{argon2}somehash",
            "{pbkdf2}somehash",
            "{sha256}somehash",
            "{md5}somehash"
    })
    @DisplayName("실패: {bcrypt} 이외의 알 수 없거나 지원하지 않는 접두사는 거부된다")
    void rejectsUnknownPrefixes(String unknownPrefix) {
        assertThat(PasswordEncoderPolicy.isValid(unknownPrefix)).isFalse();
        assertThatThrownBy(() -> PasswordEncoderPolicy.validate(unknownPrefix))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported encoding prefix");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{ }",
            "{}hash",
            "{bcrypt",
            "bcrypt}",
            "{bcrypt}x",
            "{bcrypt}not-valid",
            "{bcrypt}$2a$10$short"
    })
    @DisplayName("실패: 비어있거나 불완전한 접두사 및 짧은 페이로드는 거부된다")
    void rejectsMalformedPrefixAndShortPayload(String malformed) {
        assertThat(PasswordEncoderPolicy.isValid(malformed)).isFalse();
        assertThatThrownBy(() -> PasswordEncoderPolicy.validate(malformed))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("실패: 페이로드에 BCrypt Base64 외의 특수문자나 잘못된 버전이 포함된 경우 거부된다")
    void rejectsInvalidCharactersAndVersions() {
        String validHash = dynamicBcryptHash(4);
        String suffix53 = validHash.substring(7);

        // 허용되지 않는 버전 ($2c$, $1$, $2x$)
        String invalidVersion1 = "$2c$10$" + suffix53;
        String invalidVersion2 = "$1a$10$" + suffix53;
        String invalidVersion3 = "$2x$10$" + suffix53;

        assertThat(PasswordEncoderPolicy.isValid("{bcrypt}" + invalidVersion1)).isFalse();
        assertThat(PasswordEncoderPolicy.isValid("{bcrypt}" + invalidVersion2)).isFalse();
        assertThat(PasswordEncoderPolicy.isValid("{bcrypt}" + invalidVersion3)).isFalse();

        // 허용되지 않는 문자 (!, @, #, _, -)
        String invalidChars = "$2a$10$" + "!".repeat(53);
        assertThat(PasswordEncoderPolicy.isValid("{bcrypt}" + invalidChars)).isFalse();
    }

    @Test
    @DisplayName("반복성: 동일하거나 다른 입력에 대한 반복 검증이 부작용 없이 결정론적으로 동작한다")
    void repeatableVerification() {
        String raw = dynamicRawPassword();
        String validHash = "{bcrypt}" + DEFAULT_BCRYPT.encode(raw);

        for (int i = 0; i < 50; i++) {
            assertThat(PasswordEncoderPolicy.isValid(validHash)).isTrue();
            assertThat(PasswordEncoderPolicy.isValid("{noop}invalid")).isFalse();
            assertThat(PasswordEncoderPolicy.isValid("{bcrypt}x")).isFalse();
        }
    }

    @Test
    @DisplayName("보조: encode 헬퍼 메서드는 유효한 {bcrypt} 해시를 동적으로 생성한다")
    void encodeHelperGeneratesValidBcrypt() {
        String raw = dynamicRawPassword();
        String encoded = PasswordEncoderPolicy.encode(raw);

        assertThat(encoded).startsWith("{bcrypt}$2a$");
        assertThat(encoded).hasSize(68); // 8 ({bcrypt}) + 60 = 68
        assertThat(PasswordEncoderPolicy.isValid(encoded)).isTrue();

        String encodedCost4 = PasswordEncoderPolicy.encode(raw, 4);
        assertThat(encodedCost4).startsWith("{bcrypt}$2a$04$");
        assertThat(PasswordEncoderPolicy.isValid(encodedCost4)).isTrue();
    }
}
