package com.ho.account.auth.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.auth.AuthApplication;
import com.ho.account.auth.core.application.model.AuthenticationResult;
import com.ho.account.auth.core.application.port.in.AuthUseCase;
import com.ho.account.auth.core.application.port.out.AuthUserRepository;
import com.ho.account.auth.core.application.port.out.PasswordVerifierPort;
import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.auth.core.infrastructure.security.PasswordEncoderPolicy;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
        classes = AuthApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local")
class AuthLocalProfileBaselineDataTest {

    private static final String TEST_JWT_SECRET = ephemeralValue();
    private static final String TEST_INTERNAL_TOKEN = ephemeralValue();

    @DynamicPropertySource
    static void ephemeralAuthInputs(DynamicPropertyRegistry registry) {
        registry.add("auth.jwt.secret", () -> TEST_JWT_SECRET);
        registry.add("auth.internal-api.token", () -> TEST_INTERNAL_TOKEN);
    }

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private PasswordVerifierPort passwordVerifier;

    @Autowired
    private AuthUseCase authUseCase;

    @Test
    @DisplayName("local 프로파일에서 baseline 사용자(admin, auditor, user)가 초기화되어 조회된다")
    void baselineUsersExistInAuthUserRepositoryWithRoles() {
        // 1. admin 사용자 및 권한 검증
        Optional<AuthUser> adminOpt = authUserRepository.findByUsername("admin");
        assertThat(adminOpt).isPresent();
        AuthUser admin = adminOpt.get();
        assertThat(admin.getUsername()).isEqualTo("admin");
        assertThat(admin.isActive()).isTrue();
        assertThat(admin.isLocked()).isFalse();
        assertThat(PasswordEncoderPolicy.isValid(admin.getStoredPassword())).isTrue();
        assertThat(passwordVerifier.matches("Test1234!", admin.getStoredPassword())).isTrue();
        List<String> adminRoles = admin.effectiveRolesAt(Instant.now());
        assertThat(adminRoles).contains("ROLE_ADMIN", "ROLE_USER", "ROLE_FINANCE");

        // 2. auditor 사용자 및 권한 검증
        Optional<AuthUser> auditorOpt = authUserRepository.findByUsername("auditor");
        assertThat(auditorOpt).isPresent();
        AuthUser auditor = auditorOpt.get();
        assertThat(auditor.getUsername()).isEqualTo("auditor");
        assertThat(auditor.isActive()).isTrue();
        assertThat(auditor.isLocked()).isFalse();
        assertThat(PasswordEncoderPolicy.isValid(auditor.getStoredPassword())).isTrue();
        assertThat(passwordVerifier.matches("Test1234!", auditor.getStoredPassword())).isTrue();
        List<String> auditorRoles = auditor.effectiveRolesAt(Instant.now());
        assertThat(auditorRoles).contains("ROLE_AUDITOR", "ROLE_USER");

        // 3. user 사용자 및 권한 검증
        Optional<AuthUser> userOpt = authUserRepository.findByUsername("user");
        assertThat(userOpt).isPresent();
        AuthUser user = userOpt.get();
        assertThat(user.getUsername()).isEqualTo("user");
        assertThat(user.isActive()).isTrue();
        assertThat(user.isLocked()).isFalse();
        assertThat(PasswordEncoderPolicy.isValid(user.getStoredPassword())).isTrue();
        assertThat(passwordVerifier.matches("Test1234!", user.getStoredPassword())).isTrue();
        List<String> userRoles = user.effectiveRolesAt(Instant.now());
        assertThat(userRoles).contains("ROLE_USER");
    }

    @Test
    @DisplayName("baseline 사용자는 기본 비밀번호(Test1234!)로 로그인 및 토큰 발급이 성공한다")
    void baselineUsersCanAuthenticateSuccessfully() {
        AuthenticationResult adminResult = authUseCase.login(
                new AuthUseCase.LoginCommand("admin", "Test1234!", "NORMAL", null));
        assertThat(adminResult.username()).isEqualTo("admin");
        assertThat(adminResult.accessToken()).isNotBlank();
        assertThat(adminResult.roles()).contains("ROLE_ADMIN");

        AuthenticationResult auditorResult = authUseCase.login(
                new AuthUseCase.LoginCommand("auditor", "Test1234!", "NORMAL", null));
        assertThat(auditorResult.username()).isEqualTo("auditor");
        assertThat(auditorResult.accessToken()).isNotBlank();
        assertThat(auditorResult.roles()).contains("ROLE_AUDITOR");

        AuthenticationResult userResult = authUseCase.login(
                new AuthUseCase.LoginCommand("user", "Test1234!", "NORMAL", null));
        assertThat(userResult.username()).isEqualTo("user");
        assertThat(userResult.accessToken()).isNotBlank();
        assertThat(userResult.roles()).contains("ROLE_USER");
    }

    private static String ephemeralValue() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
