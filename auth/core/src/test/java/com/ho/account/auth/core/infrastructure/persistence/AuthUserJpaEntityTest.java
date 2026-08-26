package com.ho.account.auth.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthUserJpaEntityTest {

    @Test
    @DisplayName("유효한 {bcrypt} 패스워드를 가진 엔티티 생성을 허용한다")
    void allowsValidBcryptPassword() {
        AuthUserJpaEntity entity = new AuthUserJpaEntity(
                "admin",
                "{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG",
                "FIN",
                true,
                false,
                1L);

        assertThat(entity.toDomain().getUsername()).isEqualTo("admin");
        assertThat(entity.toDomain().getStoredPassword())
                .isEqualTo("{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG");
    }

    @Test
    @DisplayName("fromConfiguredUser는 유효한 {bcrypt} 패스워드를 정상 매핑한다")
    void fromConfiguredUserMapsValidEncodedUser() {
        AuthModuleProperties.User user = new AuthModuleProperties.User();
        user.setUsername("ops");
        user.setPassword("{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG");
        user.setDepartmentCode("OPS");
        user.setRoles(List.of("ROLE_OPS"));

        AuthUserJpaEntity entity = AuthUserJpaEntity.fromConfiguredUser(user);

        assertThat(entity.toDomain().getUsername()).isEqualTo("ops");
    }

    @Test
    @DisplayName("접두사가 없는 raw 패스워드는 IllegalArgumentException을 발생시킨다")
    void rejectsRawPassword() {
        AuthModuleProperties.User user = new AuthModuleProperties.User();
        user.setUsername("rawUser");
        user.setPassword("plaintextPassword");

        assertThatThrownBy(() -> AuthUserJpaEntity.fromConfiguredUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Raw passwords are not allowed");
    }

    @Test
    @DisplayName("{noop} 평문 패스워드는 IllegalArgumentException을 발생시킨다")
    void rejectsNoopPassword() {
        AuthModuleProperties.User user = new AuthModuleProperties.User();
        user.setUsername("noopUser");
        user.setPassword("{noop}plaintext");

        assertThatThrownBy(() -> AuthUserJpaEntity.fromConfiguredUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not use '{noop}' prefix");
    }

    @Test
    @DisplayName("빈 접두사 또는 malformed 접두사는 IllegalArgumentException을 발생시킨다")
    void rejectsMalformedPrefix() {
        AuthModuleProperties.User emptyPrefixUser = new AuthModuleProperties.User();
        emptyPrefixUser.setUsername("emptyUser");
        emptyPrefixUser.setPassword("{}plaintext");

        assertThatThrownBy(() -> AuthUserJpaEntity.fromConfiguredUser(emptyPrefixUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty or malformed encoding prefix");

        AuthModuleProperties.User unclosedPrefixUser = new AuthModuleProperties.User();
        unclosedPrefixUser.setUsername("unclosedUser");
        unclosedPrefixUser.setPassword("{bcrypt");

        assertThatThrownBy(() -> AuthUserJpaEntity.fromConfiguredUser(unclosedPrefixUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty or malformed encoding prefix");
    }
}
