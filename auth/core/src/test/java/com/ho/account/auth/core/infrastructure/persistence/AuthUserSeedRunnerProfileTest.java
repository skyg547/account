package com.ho.account.auth.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

class AuthUserSeedRunnerProfileTest {

    @Test
    @DisplayName("AuthUserSeedRunner 클래스는 @Profile('local') 어노테이션을 소유하여 dev/prod 환경 로드를 차단한다")
    void authUserSeedRunnerHasExplicitLocalProfileAnnotation() {
        Profile profileAnnotation = AuthUserSeedRunner.class.getAnnotation(Profile.class);

        assertThat(profileAnnotation)
                .as("AuthUserSeedRunner must be explicitly restricted with @Profile")
                .isNotNull();
        assertThat(profileAnnotation.value())
                .as("AuthUserSeedRunner must only activate under the 'local' profile")
                .containsExactly("local");
    }
}
