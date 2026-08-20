package com.ho.account.auth.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthUserJpaEntityTest {

    @Test
    @DisplayName("위임형 인코딩 프리픽스({id})가 적용된 비밀번호는 정상적으로 엔티티로 변환된다")
    void fromConfiguredUser_withEncodedPassword_succeeds() {
        AuthModuleProperties.User user = new AuthModuleProperties.User();
        user.setUsername("testuser");
        user.setPassword("{bcrypt}$2a$10$ephemeralHashValueForTestingOnly1234567890");
        user.setDepartmentCode("DEPT_HQ");
        user.setActive(true);
        user.setLocked(false);
        user.setRoles(List.of("ROLE_USER", "ROLE_ADMIN"));

        AuthUserJpaEntity entity = AuthUserJpaEntity.fromConfiguredUser(user);

        assertThat(entity).isNotNull();
        assertThat(entity.toDomain().getUsername()).isEqualTo("testuser");
        assertThat(entity.toDomain().getStoredPassword()).isEqualTo("{bcrypt}$2a$10$ephemeralHashValueForTestingOnly1234567890");
        assertThat(entity.toDomain().getDepartmentCode()).isEqualTo("DEPT_HQ");
        assertThat(entity.toDomain().getRoleAssignments()).hasSize(2);
    }

    @Test
    @DisplayName("평문(raw) 비밀번호 또는 프리픽스가 누락된 비밀번호는 IllegalArgumentException을 발생시킨다")
    void fromConfiguredUser_withRawPassword_throwsIllegalArgumentException() {
        AuthModuleProperties.User rawUser = new AuthModuleProperties.User();
        rawUser.setUsername("insecureUser");
        rawUser.setPassword("plaintextPassword123!");

        assertThatThrownBy(() -> AuthUserJpaEntity.fromConfiguredUser(rawUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must use a delegated encoding prefix");

        AuthModuleProperties.User nullPasswordUser = new AuthModuleProperties.User();
        nullPasswordUser.setUsername("nullUser");
        nullPasswordUser.setPassword(null);

        assertThatThrownBy(() -> AuthUserJpaEntity.fromConfiguredUser(nullPasswordUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must use a delegated encoding prefix");

        AuthModuleProperties.User invalidPrefixUser = new AuthModuleProperties.User();
        invalidPrefixUser.setUsername("invalidUser");
        invalidPrefixUser.setPassword("{brokenPrefixWithoutClosingBrace");

        assertThatThrownBy(() -> AuthUserJpaEntity.fromConfiguredUser(invalidPrefixUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must use a delegated encoding prefix");
    }
}
