package com.ho.account.auth.core.application.service;

import com.ho.account.auth.api.dto.LoginResponse;
import com.ho.account.auth.core.application.exception.InvalidCredentialsException;
import com.ho.account.auth.core.application.exception.UserAccessDeniedException;
import com.ho.account.auth.core.application.port.out.AuthUserQueryPort;
import com.ho.account.auth.core.application.port.out.PasswordVerifierPort;
import com.ho.account.auth.core.application.port.out.TokenIssuerPort;
import com.ho.account.auth.core.domain.model.AuthUser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

    @Test
    void returnsBearerTokenWhenCredentialsAreValid() {
        AuthUserQueryPort userQueryPort = new InMemoryUserQueryPort(Map.of(
                "admin", new AuthUser("admin", "{noop}1234", true, false, List.of("ROLE_ADMIN"))));
        PasswordVerifierPort passwordVerifierPort = (raw, stored) -> "{noop}".concat(raw).equals(stored);
        TokenIssuerPort tokenIssuerPort = user -> new TokenIssuerPort.IssuedToken("token-123", 3600L);
        AuthService authService = new AuthService(userQueryPort, passwordVerifierPort, tokenIssuerPort);

        LoginResponse response = authService.login("admin", "1234");

        assertThat(response.token()).isEqualTo("token-123");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        assertThat(response.username()).isEqualTo("admin");
        assertThat(response.roles()).containsExactly("ROLE_ADMIN");
    }

    @Test
    void throwsWhenUserDoesNotExist() {
        AuthService authService = new AuthService(
                new InMemoryUserQueryPort(Map.of()),
                (raw, stored) -> true,
                user -> new TokenIssuerPort.IssuedToken("token", 1L));

        assertThatThrownBy(() -> authService.login("missing", "1234"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void throwsWhenPasswordIsWrong() {
        AuthUserQueryPort userQueryPort = new InMemoryUserQueryPort(Map.of(
                "admin", new AuthUser("admin", "1234", true, false, List.of("ROLE_ADMIN"))));
        AuthService authService = new AuthService(
                userQueryPort,
                (raw, stored) -> false,
                user -> new TokenIssuerPort.IssuedToken("token", 1L));

        assertThatThrownBy(() -> authService.login("admin", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void throwsWhenUserIsInactive() {
        AuthUserQueryPort userQueryPort = new InMemoryUserQueryPort(Map.of(
                "admin", new AuthUser("admin", "1234", false, false, List.of("ROLE_ADMIN"))));
        AuthService authService = new AuthService(
                userQueryPort,
                String::equals,
                user -> new TokenIssuerPort.IssuedToken("token", 1L));

        assertThatThrownBy(() -> authService.login("admin", "1234"))
                .isInstanceOf(UserAccessDeniedException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void throwsWhenUserIsLocked() {
        AuthUserQueryPort userQueryPort = new InMemoryUserQueryPort(Map.of(
                "admin", new AuthUser("admin", "1234", true, true, List.of("ROLE_ADMIN"))));
        AuthService authService = new AuthService(
                userQueryPort,
                String::equals,
                user -> new TokenIssuerPort.IssuedToken("token", 1L));

        assertThatThrownBy(() -> authService.login("admin", "1234"))
                .isInstanceOf(UserAccessDeniedException.class)
                .hasMessageContaining("locked");
    }

    private record InMemoryUserQueryPort(Map<String, AuthUser> users) implements AuthUserQueryPort {
        @Override
        public Optional<AuthUser> findByUsername(String username) {
            return Optional.ofNullable(users.get(username));
        }
    }
}

