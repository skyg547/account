package com.ho.account.auth.core.application.service;

import com.ho.account.auth.api.dto.LoginResponse;
import com.ho.account.auth.core.application.exception.InvalidCredentialsException;
import com.ho.account.auth.core.application.exception.UserAccessDeniedException;
import com.ho.account.auth.core.application.port.out.AuthUserQueryPort;
import com.ho.account.auth.core.application.port.out.DepartmentValidationPort;
import com.ho.account.auth.core.application.port.out.LoginAttemptPort;
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
                "admin", new AuthUser("admin", "{noop}1234", "FIN", true, false, List.of("ROLE_ADMIN"))));
        DepartmentValidationPort departmentValidationPort = code -> true;
        PasswordVerifierPort passwordVerifierPort = (raw, stored) -> "{noop}".concat(raw).equals(stored);
        TokenIssuerPort tokenIssuerPort = user -> new TokenIssuerPort.IssuedToken("token-123", 3600L);
        AuthService authService = new AuthService(
                userQueryPort, departmentValidationPort, passwordVerifierPort, tokenIssuerPort, new RecordingLoginAttemptPort());

        LoginResponse response = authService.login("admin", "1234");

        assertThat(response.token()).isEqualTo("token-123");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        assertThat(response.username()).isEqualTo("admin");
        assertThat(response.departmentCode()).isEqualTo("FIN");
        assertThat(response.roles()).containsExactly("ROLE_ADMIN");
        assertThat(response.roleVersion()).isEqualTo(1L);
    }

    @Test
    void throwsWhenUserDoesNotExist() {
        AuthService authService = new AuthService(
                new InMemoryUserQueryPort(Map.of()),
                code -> true,
                (raw, stored) -> true,
                user -> new TokenIssuerPort.IssuedToken("token", 1L),
                new RecordingLoginAttemptPort());

        assertThatThrownBy(() -> authService.login("missing", "1234"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void throwsWhenPasswordIsWrong() {
        AuthUserQueryPort userQueryPort = new InMemoryUserQueryPort(Map.of(
                "admin", new AuthUser("admin", "1234", true, false, List.of("ROLE_ADMIN"))));
        AuthService authService = new AuthService(
                userQueryPort,
                code -> true,
                (raw, stored) -> false,
                user -> new TokenIssuerPort.IssuedToken("token", 1L),
                new RecordingLoginAttemptPort());

        assertThatThrownBy(() -> authService.login("admin", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void throwsWhenUserIsInactive() {
        AuthUserQueryPort userQueryPort = new InMemoryUserQueryPort(Map.of(
                "admin", new AuthUser("admin", "1234", false, false, List.of("ROLE_ADMIN"))));
        AuthService authService = new AuthService(
                userQueryPort,
                code -> true,
                String::equals,
                user -> new TokenIssuerPort.IssuedToken("token", 1L),
                new RecordingLoginAttemptPort());

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
                code -> true,
                String::equals,
                user -> new TokenIssuerPort.IssuedToken("token", 1L),
                new RecordingLoginAttemptPort());

        assertThatThrownBy(() -> authService.login("admin", "1234"))
                .isInstanceOf(UserAccessDeniedException.class)
                .hasMessageContaining("locked");
    }

    @Test
    void throwsWhenDepartmentCodeDoesNotExistInMasterData() {
        AuthUserQueryPort userQueryPort = new InMemoryUserQueryPort(Map.of(
                "admin", new AuthUser("admin", "1234", "UNKNOWN", true, false, List.of("ROLE_ADMIN"))));
        DepartmentValidationPort departmentValidationPort = code -> false;
        AuthService authService = new AuthService(
                userQueryPort,
                departmentValidationPort,
                String::equals,
                user -> new TokenIssuerPort.IssuedToken("token", 1L),
                new RecordingLoginAttemptPort());

        assertThatThrownBy(() -> authService.login("admin", "1234"))
                .isInstanceOf(UserAccessDeniedException.class)
                .hasMessageContaining("Department code is invalid");
    }

    @Test
    void validateTokenVersion_returnsTrueOnlyWhenRoleVersionMatchesCurrentUserVersion() {
        AuthUserQueryPort userQueryPort = new InMemoryUserQueryPort(Map.of(
                "admin", new AuthUser("admin", "1234", "FIN", true, false, List.of("ROLE_ADMIN")),
                "changed", new AuthUser("changed", "1234", "FIN", true, false, List.of("ROLE_ADMIN"))));
        AuthService authService = new AuthService(
                userQueryPort,
                code -> true,
                String::equals,
                user -> new TokenIssuerPort.IssuedToken("token", 1L),
                new RecordingLoginAttemptPort());

        assertThat(authService.validateTokenVersion("admin", 1L)).isTrue();
        assertThat(authService.validateTokenVersion("admin", 0L)).isFalse();
        assertThat(authService.validateTokenVersion("admin", 2L)).isFalse();
        assertThat(authService.validateTokenVersion("missing", 1L)).isFalse();
    }

    @Test
    void blocksLoginBeforePasswordVerificationWhenAttemptPolicyIsLocked() {
        RecordingLoginAttemptPort attempts = new RecordingLoginAttemptPort();
        attempts.locked = true;
        AuthService authService = new AuthService(
                new InMemoryUserQueryPort(Map.of()),
                code -> true,
                (raw, stored) -> true,
                user -> new TokenIssuerPort.IssuedToken("token", 1L),
                attempts);

        assertThatThrownBy(() -> authService.login("admin", "1234"))
                .isInstanceOf(UserAccessDeniedException.class)
                .hasMessageContaining("temporarily locked");
    }

    private static final class RecordingLoginAttemptPort implements LoginAttemptPort {
        private boolean locked;

        @Override
        public boolean isLocked(String username) {
            return locked;
        }

        @Override
        public void recordFailure(String username, String reason) {
        }

        @Override
        public void recordSuccess(String username) {
        }
    }

    private record InMemoryUserQueryPort(Map<String, AuthUser> users) implements AuthUserQueryPort {
        @Override
        public Optional<AuthUser> findByUsername(String username) {
            return Optional.ofNullable(users.get(username));
        }
    }
}
