package com.ho.account.auth.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.auth.core.application.exception.InvalidCredentialsException;
import com.ho.account.auth.core.application.exception.UserAccessDeniedException;
import com.ho.account.auth.core.application.model.AuthenticationResult;
import com.ho.account.auth.core.application.port.in.AuthUseCase;
import com.ho.account.auth.core.application.port.out.AuthUserQueryPort;
import com.ho.account.auth.core.application.port.out.DepartmentValidationPort;
import com.ho.account.auth.core.application.port.out.LoginAttemptPort;
import com.ho.account.auth.core.application.port.out.PasswordVerifierPort;
import com.ho.account.auth.core.application.port.out.TokenIssuerPort;
import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.auth.core.domain.model.RoleAssignment;
import com.ho.account.auth.core.infrastructure.security.PasswordEncoderPolicy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class AuthServiceTest {

    private static final Instant AUTHENTICATED_AT = Instant.parse("2026-07-14T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(AUTHENTICATED_AT, ZoneOffset.UTC);

    @Test
    void returnsCoreAuthenticationResultUsingOneRoleSnapshot() {
        RoleAssignment effective = new RoleAssignment(
                "ROLE_ADMIN", "FIN", AUTHENTICATED_AT.minusSeconds(1), AUTHENTICATED_AT.plusSeconds(1), true);
        RoleAssignment future = new RoleAssignment(
                "ROLE_FUTURE", "FIN", AUTHENTICATED_AT.plusSeconds(1), null, true);
        String dynamicPassword = PasswordEncoderPolicy.encode(UUID.randomUUID().toString(), 4);
        AuthUserQueryPort userQueryPort = users(Map.of(
                "admin", new AuthUser(
                        "admin", dynamicPassword, "FIN", true, false, List.of(effective, future), 3L)));
        AtomicReference<TokenIssuerPort.TokenSubject> issuedSubject = new AtomicReference<>();
        AtomicReference<Instant> issuedAt = new AtomicReference<>();
        TokenIssuerPort tokenIssuerPort = (subject, instant) -> {
            issuedSubject.set(subject);
            issuedAt.set(instant);
            return new TokenIssuerPort.IssuedToken("token-123", 3600L);
        };
        AuthService authService = service(
                userQueryPort,
                code -> true,
                (raw, stored) -> "1234".equals(raw),
                tokenIssuerPort,
                new RecordingLoginAttemptPort());

        AuthenticationResult result = authService.login(new AuthUseCase.LoginCommand(" admin ", "1234", "NORMAL", null));

        assertThat(result.accessToken()).isEqualTo("token-123");
        assertThat(result.expiresInSeconds()).isEqualTo(3600L);
        assertThat(result.username()).isEqualTo("admin");
        assertThat(result.departmentCode()).isEqualTo("FIN");
        assertThat(result.roles()).containsExactly("ROLE_ADMIN");
        assertThat(result.roleVersion()).isEqualTo(3L);
        assertThat(issuedAt.get()).isEqualTo(AUTHENTICATED_AT);
        assertThat(issuedSubject.get().effectiveRoleAssignments()).containsExactly(effective);
    }

    @Test
    void throwsWhenUserDoesNotExist() {
        AuthService authService = service(
                users(Map.of()),
                code -> true,
                (raw, stored) -> true,
                tokenIssuer(),
                new RecordingLoginAttemptPort());

        assertThatThrownBy(() -> authService.login(new AuthUseCase.LoginCommand("missing", "1234", "NORMAL", null)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void throwsWhenPasswordIsWrong() {
        AuthService authService = service(
                users(Map.of("admin", user(true, false, List.of(RoleAssignment.approved("ROLE_ADMIN"))))),
                code -> true,
                (raw, stored) -> false,
                tokenIssuer(),
                new RecordingLoginAttemptPort());

        assertThatThrownBy(() -> authService.login(new AuthUseCase.LoginCommand("admin", "wrong", "NORMAL", null)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsSsoBeforeCredentialAdaptersWhenProviderIsUnavailable() {
        AtomicBoolean userQueried = new AtomicBoolean();
        AtomicBoolean passwordVerified = new AtomicBoolean();
        AtomicBoolean tokenIssued = new AtomicBoolean();
        RecordingLoginAttemptPort attempts = new RecordingLoginAttemptPort();
        AuthService authService = service(
                username -> {
                    userQueried.set(true);
                    return Optional.of(user(true, false, List.of(RoleAssignment.approved("ROLE_ADMIN"))));
                },
                code -> true,
                (raw, stored) -> {
                    passwordVerified.set(true);
                    return true;
                },
                (subject, issuedAt) -> {
                    tokenIssued.set(true);
                    return new TokenIssuerPort.IssuedToken("token", 1L);
                },
                attempts);

        assertThatThrownBy(() -> authService.login(new AuthUseCase.LoginCommand("admin", "", "SSO", null)))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(userQueried).isFalse();
        assertThat(passwordVerified).isFalse();
        assertThat(tokenIssued).isFalse();
        assertThat(attempts.isLockedCalls).isZero();
        assertThat(attempts.recordFailureCalls).isZero();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "KERBEROS"})
    void rejectsRepeatedUnsupportedLoginTypesBeforeAttemptPolicyAndCredentialAdapters(String loginType) {
        AtomicBoolean userQueried = new AtomicBoolean();
        AtomicBoolean passwordVerified = new AtomicBoolean();
        AtomicBoolean tokenIssued = new AtomicBoolean();
        RecordingLoginAttemptPort attempts = new RecordingLoginAttemptPort();
        AuthService authService = service(
                username -> {
                    userQueried.set(true);
                    return Optional.of(user(true, false, List.of(RoleAssignment.approved("ROLE_ADMIN"))));
                },
                code -> true,
                (raw, stored) -> {
                    passwordVerified.set(true);
                    return true;
                },
                (subject, issuedAt) -> {
                    tokenIssued.set(true);
                    return new TokenIssuerPort.IssuedToken("token", 1L);
                },
                attempts);

        for (int attempt = 0; attempt < 6; attempt++) {
            assertThatThrownBy(
                            () -> authService.login(new AuthUseCase.LoginCommand("admin", "1234", loginType, null)))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
        assertThat(userQueried).isFalse();
        assertThat(passwordVerified).isFalse();
        assertThat(tokenIssued).isFalse();
        assertThat(attempts.isLockedCalls).isZero();
        assertThat(attempts.recordFailureCalls).isZero();
    }

    @Test
    void acceptsNormalLoginTypeCaseInsensitively() {
        AuthService authService = service(
                users(Map.of("admin", user(true, false, List.of(RoleAssignment.approved("ROLE_ADMIN"))))),
                code -> true,
                String::equals,
                tokenIssuer(),
                new RecordingLoginAttemptPort());

        AuthenticationResult result =
                authService.login(new AuthUseCase.LoginCommand("admin", "1234", "normal", null));

        assertThat(result.accessToken()).isEqualTo("token");
    }

    @Test
    void rejectsLegacyFixedOtpForLdapWithoutIssuingToken() {
        AtomicBoolean tokenIssued = new AtomicBoolean();
        RecordingLoginAttemptPort attempts = new RecordingLoginAttemptPort();
        AuthService authService = service(
                users(Map.of("admin", user(true, false, List.of(RoleAssignment.approved("ROLE_ADMIN"))))),
                code -> true,
                String::equals,
                (subject, issuedAt) -> {
                    tokenIssued.set(true);
                    return new TokenIssuerPort.IssuedToken("token", 1L);
                },
                attempts);

        assertThatThrownBy(() -> authService.login(new AuthUseCase.LoginCommand("admin", "1234", "LDAP", "123456")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(tokenIssued).isFalse();
        assertThat(attempts.lastFailureReason).isEqualTo("LDAP_OTP_VERIFIER_UNAVAILABLE");
    }

    @Test
    void rejectsAnyOtherOtpForLdapWithoutIssuingToken() {
        AtomicBoolean tokenIssued = new AtomicBoolean();
        RecordingLoginAttemptPort attempts = new RecordingLoginAttemptPort();
        AuthService authService = service(
                users(Map.of("admin", user(true, false, List.of(RoleAssignment.approved("ROLE_ADMIN"))))),
                code -> true,
                String::equals,
                (subject, issuedAt) -> {
                    tokenIssued.set(true);
                    return new TokenIssuerPort.IssuedToken("token", 1L);
                },
                attempts);

        assertThatThrownBy(() -> authService.login(new AuthUseCase.LoginCommand("admin", "1234", "LDAP", "654321")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(tokenIssued).isFalse();
        assertThat(attempts.lastFailureReason).isEqualTo("LDAP_OTP_VERIFIER_UNAVAILABLE");
    }

    @Test
    void throwsWhenUserIsInactive() {
        AuthService authService = service(
                users(Map.of("admin", user(false, false, List.of(RoleAssignment.approved("ROLE_ADMIN"))))),
                code -> true,
                String::equals,
                tokenIssuer(),
                new RecordingLoginAttemptPort());

        assertThatThrownBy(() -> authService.login(new AuthUseCase.LoginCommand("admin", "1234", "NORMAL", null)))
                .isInstanceOf(UserAccessDeniedException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void throwsWhenUserIsLocked() {
        AuthService authService = service(
                users(Map.of("admin", user(true, true, List.of(RoleAssignment.approved("ROLE_ADMIN"))))),
                code -> true,
                String::equals,
                tokenIssuer(),
                new RecordingLoginAttemptPort());

        assertThatThrownBy(() -> authService.login(new AuthUseCase.LoginCommand("admin", "1234", "NORMAL", null)))
                .isInstanceOf(UserAccessDeniedException.class)
                .hasMessageContaining("locked");
    }

    @Test
    void throwsWhenDepartmentCodeDoesNotExistInMasterData() {
        AuthService authService = service(
                users(Map.of("admin", new AuthUser(
                        "admin", "1234", "UNKNOWN", true, false, List.of("ROLE_ADMIN")))),
                code -> false,
                String::equals,
                tokenIssuer(),
                new RecordingLoginAttemptPort());

        assertThatThrownBy(() -> authService.login(new AuthUseCase.LoginCommand("admin", "1234", "NORMAL", null)))
                .isInstanceOf(UserAccessDeniedException.class)
                .hasMessageContaining("Department code is invalid");
    }

    @Test
    void throwsWhenNoRoleIsEffectiveAtAuthenticationTime() {
        RoleAssignment expired = new RoleAssignment(
                "ROLE_ADMIN", "GLOBAL", null, AUTHENTICATED_AT, true);
        AuthService authService = service(
                users(Map.of("admin", user(true, false, List.of(expired)))),
                code -> true,
                String::equals,
                tokenIssuer(),
                new RecordingLoginAttemptPort());

        assertThatThrownBy(() -> authService.login(new AuthUseCase.LoginCommand("admin", "1234", "NORMAL", null)))
                .isInstanceOf(UserAccessDeniedException.class)
                .hasMessageContaining("no approved effective roles");
    }

    @Test
    void validateTokenVersionRequiresMatchingVersionAndAvailableAccount() {
        Map<String, AuthUser> userMap = Map.of(
                "active", user(true, false, List.of(RoleAssignment.approved("ROLE_ADMIN"))),
                "inactive", user(false, false, List.of(RoleAssignment.approved("ROLE_ADMIN"))),
                "locked", user(true, true, List.of(RoleAssignment.approved("ROLE_ADMIN"))),
                "expired", user(true, false, List.of(new RoleAssignment(
                        "ROLE_ADMIN", "GLOBAL", null, AUTHENTICATED_AT, true))));
        AuthService authService = service(
                users(userMap),
                code -> true,
                String::equals,
                tokenIssuer(),
                new RecordingLoginAttemptPort());

        assertThat(authService.validateTokenVersion("active", 1L)).isTrue();
        assertThat(authService.validateTokenVersion("active", 0L)).isFalse();
        assertThat(authService.validateTokenVersion("active", 2L)).isFalse();
        assertThat(authService.validateTokenVersion("inactive", 1L)).isFalse();
        assertThat(authService.validateTokenVersion("locked", 1L)).isFalse();
        assertThat(authService.validateTokenVersion("expired", 1L)).isFalse();
        assertThat(authService.validateTokenVersion("missing", 1L)).isFalse();
    }

    @Test
    void blocksLoginBeforePasswordVerificationWhenAttemptPolicyIsLocked() {
        RecordingLoginAttemptPort attempts = new RecordingLoginAttemptPort();
        attempts.locked = true;
        AuthService authService = service(
                users(Map.of()),
                code -> true,
                (raw, stored) -> true,
                tokenIssuer(),
                attempts);

        assertThatThrownBy(() -> authService.login(new AuthUseCase.LoginCommand("admin", "1234", "NORMAL", null)))
                .isInstanceOf(UserAccessDeniedException.class)
                .hasMessageContaining("temporarily locked");
    }

    @Test
    void rejectsMissingCoreLoginInputBeforeCallingAdapters() {
        AuthService authService = service(
                users(Map.of()),
                code -> true,
                (raw, stored) -> true,
                tokenIssuer(),
                new RecordingLoginAttemptPort());

        assertThatThrownBy(() -> authService.login(new AuthUseCase.LoginCommand(" ", "1234", "NORMAL", null)))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> authService.login(null))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    private AuthService service(
            AuthUserQueryPort userQueryPort,
            DepartmentValidationPort departmentValidationPort,
            PasswordVerifierPort passwordVerifierPort,
            TokenIssuerPort tokenIssuerPort,
            LoginAttemptPort loginAttemptPort) {
        return new AuthService(
                userQueryPort,
                departmentValidationPort,
                passwordVerifierPort,
                tokenIssuerPort,
                loginAttemptPort,
                CLOCK);
    }

    private TokenIssuerPort tokenIssuer() {
        return (subject, issuedAt) -> new TokenIssuerPort.IssuedToken("token", 1L);
    }

    private AuthUser user(boolean active, boolean locked, List<RoleAssignment> assignments) {
        return new AuthUser("admin", "1234", "FIN", active, locked, assignments, 1L);
    }

    private AuthUserQueryPort users(Map<String, AuthUser> users) {
        return username -> Optional.ofNullable(users.get(username));
    }

    private static final class RecordingLoginAttemptPort implements LoginAttemptPort {
        private boolean locked;
        private int isLockedCalls;
        private int recordFailureCalls;
        private String lastFailureReason;

        @Override
        public boolean isLocked(String username) {
            isLockedCalls++;
            return locked;
        }

        @Override
        public void recordFailure(String username, String reason) {
            recordFailureCalls++;
            lastFailureReason = reason;
        }

        @Override
        public void recordSuccess(String username) {
        }
    }
}
