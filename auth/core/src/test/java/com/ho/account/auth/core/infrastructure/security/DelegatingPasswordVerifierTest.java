package com.ho.account.auth.core.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class DelegatingPasswordVerifierTest {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final DelegatingPasswordVerifier verifier = new DelegatingPasswordVerifier();

    @Test
    @DisplayName("정확한 정책 형식만 원문과 일치하고 잘못된 원문에는 false를 반환한다")
    void matchesOnlyApprovedStoredCredential() {
        String raw = randomRaw();
        String stored = PasswordEncoderPolicy.encode(raw, 4);

        assertThat(verifier.matches(raw, stored)).isTrue();
        assertThat(verifier.matches(randomRaw(), stored)).isFalse();
    }

    @Test
    @DisplayName("정책과 동일하게 prefixless, 공백 래핑, 대소문자 변형을 false로 차단한다")
    void rejectsPolicyVariantsWithoutThrowing() {
        String raw = randomRaw();
        String payload = new BCryptPasswordEncoder(4).encode(raw);
        List<String> rejected = List.of(
                payload,
                " " + PasswordEncoderPolicy.BCRYPT_PREFIX + payload,
                PasswordEncoderPolicy.BCRYPT_PREFIX + payload + " ",
                "{BCRYPT}" + payload,
                "{Bcrypt}" + payload);

        rejected.forEach(stored -> {
            assertThatCode(() -> verifier.matches(raw, stored)).doesNotThrowAnyException();
            assertThat(verifier.matches(raw, stored)).isFalse();
        });
    }

    @Test
    @DisplayName("평문/noop/unknown/malformed 입력은 예외나 정보 공개 없이 false를 반환한다")
    void unsafeStoredCredentialsFailClosed() {
        String raw = randomRaw();
        String payload = new BCryptPasswordEncoder(4).encode(raw);
        String unknownId = "x" + randomRaw().substring(0, 8);

        List.of(
                        raw,
                        "{noop}" + raw,
                        "{" + unknownId + "}" + payload,
                        PasswordEncoderPolicy.BCRYPT_PREFIX + randomRaw(),
                        PasswordEncoderPolicy.BCRYPT_PREFIX + "$2a$03$" + payload.substring(7))
                .forEach(stored -> {
                    assertThatCode(() -> verifier.matches(raw, stored)).doesNotThrowAnyException();
                    assertThat(verifier.matches(raw, stored)).isFalse();
                });
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t\n"})
    @DisplayName("null 또는 공백 원문/저장값은 false를 반환한다")
    void rejectsNullOrBlank(String blank) {
        String raw = randomRaw();
        String stored = PasswordEncoderPolicy.encode(raw, 4);

        assertThat(verifier.matches(blank, stored)).isFalse();
        assertThat(verifier.matches(raw, blank)).isFalse();
    }

    private static String randomRaw() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
