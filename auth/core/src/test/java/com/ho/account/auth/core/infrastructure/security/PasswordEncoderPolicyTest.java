package com.ho.account.auth.core.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordEncoderPolicyTest {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String FIELD_PATH = "auth.users[0].password";

    @Test
    @DisplayName("정확한 소문자 {bcrypt} 접두사와 구조적으로 유효한 BCrypt만 허용한다")
    void acceptsOnlyExactLowercaseBcryptPrefix() {
        String payload = bcryptPayload(randomRaw(), 4);
        String stored = PasswordEncoderPolicy.BCRYPT_PREFIX + payload;

        assertThat(PasswordEncoderPolicy.isValid(stored)).isTrue();
        PasswordEncoderPolicy.validate(stored, FIELD_PATH);
        assertThat(PasswordEncoderPolicy.extractPayload(stored)).isEqualTo(payload);
        assertThat(PasswordEncoderPolicy.formatWithPrefix(payload)).isEqualTo(stored);
    }

    @Test
    @DisplayName("접두사 없는 BCrypt, 주변 공백, 대문자/혼합 접두사를 모두 거부한다")
    void rejectsPrefixlessWhitespaceWrappedAndCaseVariantBcrypt() {
        String payload = bcryptPayload(randomRaw(), 4);
        List<String> rejected = List.of(
                payload,
                " " + PasswordEncoderPolicy.BCRYPT_PREFIX + payload,
                PasswordEncoderPolicy.BCRYPT_PREFIX + payload + " ",
                "{BCRYPT}" + payload,
                "{Bcrypt}" + payload);

        rejected.forEach(candidate -> {
            assertThat(PasswordEncoderPolicy.isValid(candidate)).isFalse();
            assertGenericFailure(candidate);
        });
    }

    @Test
    @DisplayName("평문, noop, 알 수 없는 접두사 및 malformed 페이로드를 입력 노출 없이 거부한다")
    void rejectsUnsafeAndMalformedCredentialsWithoutDisclosure() {
        String raw = randomRaw();
        String payload = bcryptPayload(raw, 4);
        String unknownId = "x" + randomRaw().substring(0, 8);
        String malformedPayload = randomRaw();

        List.of(
                        raw,
                        "{noop}" + raw,
                        "{" + unknownId + "}" + payload,
                        PasswordEncoderPolicy.BCRYPT_PREFIX + malformedPayload)
                .forEach(candidate -> assertGenericFailure(candidate, raw, unknownId, malformedPayload));
    }

    @Test
    @DisplayName("범위를 벗어난 cost는 숫자나 payload를 노출하지 않고 거부한다")
    void rejectsOutOfRangeCostWithoutDisclosure() {
        String payload = bcryptPayload(randomRaw(), 4);
        String suffix = payload.substring(7);
        String belowMinimum = PasswordEncoderPolicy.BCRYPT_PREFIX + "$2a$03$" + suffix;
        String aboveMaximum = PasswordEncoderPolicy.BCRYPT_PREFIX + "$2a$32$" + suffix;

        assertGenericFailure(belowMinimum, "03", suffix);
        assertGenericFailure(aboveMaximum, "32", suffix);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t\n"})
    @DisplayName("null 또는 공백도 동일한 비노출 오류 계약으로 거부한다")
    void rejectsNullOrBlank(String candidate) {
        assertThat(PasswordEncoderPolicy.isValid(candidate)).isFalse();
        assertThatThrownBy(() -> PasswordEncoderPolicy.validate(candidate, FIELD_PATH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(FIELD_PATH)
                .hasMessageContaining("{bcrypt}");
    }

    @Test
    @DisplayName("동적 encode 결과는 승인된 저장 형식을 만들고 원문과 일치한다")
    void encodeCreatesApprovedCredential() {
        String raw = randomRaw();
        String encoded = PasswordEncoderPolicy.encode(raw, 4);

        assertThat(encoded).startsWith(PasswordEncoderPolicy.BCRYPT_PREFIX);
        assertThat(PasswordEncoderPolicy.isValid(encoded)).isTrue();
        assertThat(new BCryptPasswordEncoder().matches(raw, PasswordEncoderPolicy.extractPayload(encoded))).isTrue();
    }

    private static void assertGenericFailure(String candidate, String... sensitiveFragments) {
        assertThat(PasswordEncoderPolicy.isValid(candidate)).isFalse();
        assertThatThrownBy(() -> PasswordEncoderPolicy.validate(candidate, FIELD_PATH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(FIELD_PATH)
                .hasMessageContaining("{bcrypt}")
                .satisfies(error -> {
                    for (String fragment : sensitiveFragments) {
                        assertThat(error.getMessage()).doesNotContain(fragment);
                    }
                });
    }

    private static String bcryptPayload(String raw, int strength) {
        return new BCryptPasswordEncoder(strength).encode(raw);
    }

    private static String randomRaw() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
