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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(classes = AuthApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local")
class AuthLocalProfileConfiguredUserTest {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String USERNAME = "configured-" + randomValue().substring(0, 12);
    private static final String RAW_PASSWORD = randomValue();
    private static final String ENCODED_PASSWORD = PasswordEncoderPolicy.encode(RAW_PASSWORD, 4);
    private static final String ROLE = "ROLE_TEST_" + randomValue().substring(0, 8);
    private static final String JWT_SECRET = randomValue();
    private static final String INTERNAL_TOKEN = randomValue();

    @DynamicPropertySource
    static void runtimeConfiguredUser(DynamicPropertyRegistry registry) {
        registry.add("auth.jwt.secret", () -> JWT_SECRET);
        registry.add("auth.internal-api.token", () -> INTERNAL_TOKEN);
        registry.add("auth.users[0].username", () -> USERNAME);
        registry.add("auth.users[0].password", () -> ENCODED_PASSWORD);
        registry.add("auth.users[0].roles[0]", () -> ROLE);
    }

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private PasswordVerifierPort passwordVerifier;

    @Autowired
    private AuthUseCase authUseCase;

    @Test
    @DisplayName("local profile은 런타임 configured user만 seed하고 승인된 credential로 인증한다")
    void seedsAndAuthenticatesRuntimeConfiguredUser() {
        AuthUser user = authUserRepository.findByUsername(USERNAME).orElseThrow();

        assertThat(user.getStoredPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(passwordVerifier.matches(RAW_PASSWORD, user.getStoredPassword())).isTrue();
        assertThat(user.effectiveRolesAt(Instant.now())).containsExactly(ROLE);

        AuthenticationResult result = authUseCase.login(
                new AuthUseCase.LoginCommand(USERNAME, RAW_PASSWORD, "NORMAL", null));
        assertThat(result.username()).isEqualTo(USERNAME);
        assertThat(result.roles()).containsExactly(ROLE);
        assertThat(result.accessToken()).isNotBlank();
    }

    private static String randomValue() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
