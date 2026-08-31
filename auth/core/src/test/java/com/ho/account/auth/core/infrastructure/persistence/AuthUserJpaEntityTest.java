package com.ho.account.auth.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthUserJpaEntityTest {

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder(4);

    private static String dynamicBcryptHash() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return "{bcrypt}" + BCRYPT.encode(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }

    @Test
    @DisplayName("정상: 동적으로 생성된 {bcrypt} 비밀번호로 엔티티가 정상 생성된다")
    void createsEntityWithValidBcryptPassword() {
        String password = dynamicBcryptHash();
        AuthUserJpaEntity entity = new AuthUserJpaEntity("admin", password, "FIN", true, false, 1L);

        assertThat(entity.toDomain().getUsername()).isEqualTo("admin");
        assertThat(entity.toDomain().getStoredPassword()).isEqualTo(password);
    }

    @Test
    @DisplayName("정상: fromConfiguredUser 팩토리를 통해 유효한 사용자가 정상 생성된다")
    void createsEntityFromConfiguredUser() {
        String password = dynamicBcryptHash();
        AuthModuleProperties.User user = new AuthModuleProperties.User();
        user.setUsername("teller");
        user.setPassword(password);
        user.setDepartmentCode("BR001");
        user.setRoles(List.of("ROLE_TELLER"));

        AuthUserJpaEntity entity = AuthUserJpaEntity.fromConfiguredUser(user);

        assertThat(entity.toDomain().getUsername()).isEqualTo("teller");
        assertThat(entity.toDomain().getStoredPassword()).isEqualTo(password);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t\n  "})
    @DisplayName("실패: null 또는 공백 비밀번호는 거부된다")
    void rejectsNullOrBlankPassword(String blankPassword) {
        assertThatThrownBy(() -> new AuthUserJpaEntity("admin", blankPassword, "FIN", true, false, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("storedPassword is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "plaintext1234",
            "1234",
            "adminPassword!"
    })
    @DisplayName("실패: 평문 및 raw 패스워드는 거부된다")
    void rejectsRawPlaintextPassword(String rawPassword) {
        assertThatThrownBy(() -> new AuthUserJpaEntity("admin", rawPassword, "FIN", true, false, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{noop}1234",
            "{noop}plaintext",
            "{NOOP}password"
    })
    @DisplayName("실패: {noop} 접두사는 보안 정책상 거부된다")
    void rejectsNoopPassword(String noopPassword) {
        assertThatThrownBy(() -> new AuthUserJpaEntity("admin", noopPassword, "FIN", true, false, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("forbidden '{noop}' prefix");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{unknown}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG",
            "{argon2}hash",
            "{pbkdf2}hash"
    })
    @DisplayName("실패: {bcrypt} 이외의 알 수 없는 접두사는 거부된다")
    void rejectsUnknownPrefixPassword(String unknownPrefix) {
        assertThatThrownBy(() -> new AuthUserJpaEntity("admin", unknownPrefix, "FIN", true, false, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported encoding prefix");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{bcrypt}x",
            "{bcrypt}not-valid",
            "{bcrypt}$2a$10$short",
            "{}",
            "{bcrypt"
    })
    @DisplayName("실패: malformed BCrypt 페이로드는 거부된다")
    void rejectsMalformedPayload(String malformed) {
        assertThatThrownBy(() -> new AuthUserJpaEntity("admin", malformed, "FIN", true, false, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("실패: fromConfiguredUser에 null 전달 시 IllegalArgumentException이 발생한다")
    void fromConfiguredUserRejectsNull() {
        assertThatThrownBy(() -> AuthUserJpaEntity.fromConfiguredUser(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user is required");
    }
}
