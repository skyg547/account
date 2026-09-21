package com.ho.account.auth.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import com.ho.account.auth.core.infrastructure.security.PasswordEncoderPolicy;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthUserJpaEntityTest {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Test
    @DisplayName("동적 승인 credential을 가진 configured user를 entity로 매핑한다")
    void createsEntityFromConfiguredUser() {
        String username = randomUsername();
        String password = PasswordEncoderPolicy.encode(randomValue(), 4);
        AuthModuleProperties.User user = user(username, password);

        AuthUserJpaEntity entity = AuthUserJpaEntity.fromConfiguredUser(user);

        assertThat(entity.toDomain().getUsername()).isEqualTo(username);
        assertThat(entity.toDomain().getStoredPassword()).isEqualTo(password);
    }

    @Test
    @DisplayName("null configured user를 stable field 오류로 거부한다")
    void rejectsNullConfiguredUser() {
        assertThatThrownBy(() -> AuthUserJpaEntity.fromConfiguredUser(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user");
    }

    @Test
    @DisplayName("entity 생성 경계도 raw/noop/unknown/malformed/prefix 변형을 거부하고 입력을 노출하지 않는다")
    void rejectsInvalidStoredCredentialWithoutDisclosure() {
        String raw = randomValue();
        String valid = PasswordEncoderPolicy.encode(raw, 4);
        String payload = valid.substring(PasswordEncoderPolicy.BCRYPT_PREFIX.length());
        String unknownId = "x" + randomValue().substring(0, 8);

        List.of(
                        raw,
                        "{noop}" + raw,
                        "{" + unknownId + "}" + payload,
                        PasswordEncoderPolicy.BCRYPT_PREFIX + randomValue(),
                        payload,
                        "{BCRYPT}" + payload,
                        valid + " ")
                .forEach(candidate -> assertThatThrownBy(() -> AuthUserJpaEntity.fromConfiguredUser(
                                user(randomUsername(), candidate)))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("storedPassword")
                        .hasMessageContaining("{bcrypt}")
                        .satisfies(error -> assertThat(error.getMessage())
                                .doesNotContain(raw, unknownId, payload, candidate)));
    }

    private static AuthModuleProperties.User user(String username, String password) {
        AuthModuleProperties.User user = new AuthModuleProperties.User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRoles(List.of("ROLE_TEST_" + randomValue().substring(0, 8)));
        return user;
    }

    private static String randomUsername() {
        return "configured-" + randomValue().substring(0, 12);
    }

    private static String randomValue() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
