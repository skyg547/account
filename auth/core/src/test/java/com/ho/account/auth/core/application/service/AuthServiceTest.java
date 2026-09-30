package com.ho.account.auth.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.auth.core.application.exception.InvalidCredentialsException;
import com.ho.account.auth.core.application.exception.UserAccessDeniedException;
import com.ho.account.auth.core.application.model.AuthenticationResult;
import com.ho.account.auth.core.application.model.SsoUserProfile;
import com.ho.account.auth.core.application.port.in.AuthUseCase;
import com.ho.account.auth.core.application.port.out.AuthUserQueryPort;
import com.ho.account.auth.core.application.port.out.DepartmentValidationPort;
import com.ho.account.auth.core.application.port.out.LoginAttemptPort;
import com.ho.account.auth.core.application.port.out.OtpVerificationPort;
import com.ho.account.auth.core.application.port.out.PasswordVerifierPort;
import com.ho.account.auth.core.application.port.out.SsoAuthenticationPort;
import com.ho.account.auth.core.application.port.out.TokenIssuerPort;
import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.auth.core.domain.model.RoleAssignment;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import com.ho.account.auth.core.infrastructure.security.InMemoryLoginAttemptAdapter;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class AuthServiceTest {

    private static final Instant AUTHENTICATED_AT = Instant.parse("2026-07-14T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(AUTHENTICATED_AT, ZoneOffset.UTC);
    private final String validPassword = UUID.randomUUID().toString();
    private final String validOtp = "%06d".formatted(ThreadLocalRandom.current().nextInt(100_000, 1_000_000));
    private final String validSsoToken = UUID.randomUUID().toString();

    @Test
    void normalLoginWithoutOtpUsesOneInternalRoleSnapshot() {
        RoleAssignment effective = new RoleAssignment(
                "ROLE_ADMIN", "GLOBAL", AUTHENTICATED_AT.minusSeconds(1), AUTHENTICATED_AT.plusSeconds(1), true);
        RoleAssignment future = new RoleAssignment(
                "ROLE_FUTURE", "GLOBAL", AUTHENTICATED_AT.plusSeconds(1), null, true);
        AuthUserQueryPort users = users(Map.of(
                "admin", new AuthUser("admin", "stored", "FIN", true, false, List.of(effective, future), 3L)));
        AtomicReference<TokenIssuerPort.TokenSubject> issuedSubject = new AtomicReference<>();
        AtomicReference<Instant> issuedAt = new AtomicReference<>();
        TokenIssuerPort tokens = (subject, instant) -> {
            issuedSubject.set(subject);
            issuedAt.set(instant);
            return new TokenIssuerPort.IssuedToken("token-123", 3600L);
        };
        RecordingLoginAttemptPort attempts = new RecordingLoginAttemptPort();
        AuthService service = service(users, code -> true, validPasswordVerifier(), noOtp(), rejectingSso(), tokens, attempts);

        AuthenticationResult result = service.login(normal(" admin ", null));

        assertThat(result.accessToken()).isEqualTo("token-123");
        assertThat(result.roles()).containsExactly("ROLE_ADMIN");
        assertThat(result.roleVersion()).isEqualTo(3L);
        assertThat(issuedAt.get()).isEqualTo(AUTHENTICATED_AT);
        assertThat(issuedSubject.get().effectiveRoleAssignments()).containsExactly(effective);
        assertThat(attempts.successes).containsExactly("admin");
        assertThat(attempts.failures).isEmpty();
    }

    @Test
    void normalUserRequiringOtpSucceedsWithValidCode() {
        RecordingOtpPort otp = new RecordingOtpPort(true, validOtp);
        AuthService service = standardService(otp, rejectingSso(), new RecordingLoginAttemptPort());

        AuthenticationResult result = service.login(normal("admin", validOtp));

        assertThat(result.accessToken()).isEqualTo("token");
        assertThat(otp.lastUsername).isEqualTo("admin");
        assertThat(otp.lastCode).isEqualTo(validOtp);
    }

    @Test
    void normalUserRequiringOtpRejectsInvalidAndMissingCodeWithReasons() {
        RecordingLoginAttemptPort attempts = new RecordingLoginAttemptPort();
        AuthService service = standardService(new RecordingOtpPort(true, validOtp), rejectingSso(), attempts);

        assertThatThrownBy(() -> service.login(normal("admin", "000000")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(attempts.lastFailureReason()).isEqualTo("INVALID_OTP");

        assertThatThrownBy(() -> service.login(normal("admin", null)))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(attempts.lastFailureReason()).isEqualTo("OTP_REQUIRED");
    }

    @Test
    void suppliedOtpOnOtherwiseOptionalNormalLoginIsAlwaysVerified() {
        RecordingOtpPort otp = new RecordingOtpPort(false, validOtp);
        AuthService service = standardService(otp, rejectingSso(), new RecordingLoginAttemptPort());

        service.login(normal("admin", validOtp));

        assertThat(otp.lastCode).isEqualTo(validOtp);
    }

    @Test
    void ldapLoginRequiresOtpAndAcceptsOnlyVerifiedCode() {
        RecordingLoginAttemptPort attempts = new RecordingLoginAttemptPort();
        AuthService service = standardService(new RecordingOtpPort(false, validOtp), rejectingSso(), attempts);

        assertThat(service.login(ldap(validOtp)).accessToken()).isEqualTo("token");
        assertThatThrownBy(() -> service.login(ldap("000000")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(attempts.lastFailureReason()).isEqualTo("INVALID_OTP");
        assertThatThrownBy(() -> service.login(ldap(null)))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(attempts.lastFailureReason()).isEqualTo("OTP_REQUIRED");
    }

    @Test
    void ssoLoginUsesAuthenticatedSubjectAndInternalRoles() {
        AtomicReference<TokenIssuerPort.TokenSubject> issued = new AtomicReference<>();
        SsoAuthenticationPort sso = (provider, credential) -> {
            assertThat(provider).isEqualTo("corporate-oidc");
            assertThat(credential).isEqualTo(validSsoToken);
            return new SsoUserProfile(
                    "admin", "admin@example.test", "Admin", "FIN", List.of("ROLE_PROVIDER_ADMIN"));
        };
        AuthService service = service(
                standardUsers(),
                code -> true,
                (raw, stored) -> { throw new AssertionError("SSO must not verify a local password"); },
                noOtp(),
                sso,
                (subject, instant) -> {
                    issued.set(subject);
                    return new TokenIssuerPort.IssuedToken("sso-token", 60L);
                },
                new RecordingLoginAttemptPort());

        AuthenticationResult result = service.login(sso("admin", validSsoToken));

        assertThat(result.roles()).containsExactly("ROLE_INTERNAL");
        assertThat(issued.get().effectiveRoleAssignments())
                .extracting(RoleAssignment::roleCode)
                .containsExactly("ROLE_INTERNAL");
    }

    @Test
    void ssoRejectsInvalidCredentialDisabledProviderAndRecordsGenericReason() {
        for (SsoAuthenticationPort failingPort : List.<SsoAuthenticationPort>of(
                (provider, credential) -> { throw new SsoAuthenticationPort.SsoAuthenticationException(); },
                (provider, credential) -> { throw new IllegalStateException("disabled"); })) {
            RecordingLoginAttemptPort attempts = new RecordingLoginAttemptPort();
            AuthService service = standardService(noOtp(), failingPort, attempts);
            String suppliedCredential = UUID.randomUUID().toString();

            assertThatThrownBy(() -> service.login(sso("admin", suppliedCredential)))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageNotContaining(suppliedCredential);
            assertThat(attempts.lastFailureReason()).isEqualTo("SSO_AUTHENTICATION_FAILED");
        }
    }

    @Test
    void ssoRejectsSubjectMismatchAndUnmappedSubject() {
        RecordingLoginAttemptPort mismatchAttempts = new RecordingLoginAttemptPort();
        AuthService mismatchService = standardService(
                noOtp(), profile("different-user", List.of("ROLE_PROVIDER")), mismatchAttempts);

        assertThatThrownBy(() -> mismatchService.login(sso("admin", validSsoToken)))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(mismatchAttempts.lastFailureReason()).isEqualTo("SSO_SUBJECT_MISMATCH");

        RecordingLoginAttemptPort unmappedAttempts = new RecordingLoginAttemptPort();
        AuthService unmappedService = service(
                users(Map.of()), code -> true, validPasswordVerifier(), noOtp(),
                profile("missing", List.of()), tokenIssuer(), unmappedAttempts);

        assertThatThrownBy(() -> unmappedService.login(sso("missing", validSsoToken)))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(unmappedAttempts.lastFailureReason()).isEqualTo("SSO_UNMAPPED_USER");
    }

    @Test
    void repeatedInvalidOtpLocksAccountUsingRealInMemoryPolicy() {
        InMemoryLoginAttemptAdapter attempts = inMemoryAttempts(2);
        AuthService service = standardService(new RecordingOtpPort(true, validOtp), rejectingSso(), attempts);

        assertThatThrownBy(() -> service.login(normal("admin", "000000")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> service.login(normal("admin", "000000")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> service.login(normal("admin", validOtp)))
                .isInstanceOf(UserAccessDeniedException.class)
                .hasMessageContaining("temporarily locked");
    }

    @Test
    void repeatedInvalidSsoCredentialsLockAccountUsingRealInMemoryPolicy() {
        InMemoryLoginAttemptAdapter attempts = inMemoryAttempts(2);
        AuthService service = standardService(noOtp(), rejectingSso(), attempts);

        assertThatThrownBy(() -> service.login(sso("admin", UUID.randomUUID().toString())))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> service.login(sso("admin", UUID.randomUUID().toString())))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> service.login(sso("admin", validSsoToken)))
                .isInstanceOf(UserAccessDeniedException.class)
                .hasMessageContaining("temporarily locked");
    }

    @Test
    void recordsPasswordAndAccountFailures() {
        RecordingLoginAttemptPort attempts = new RecordingLoginAttemptPort();
        AuthService wrongPassword = service(
                standardUsers(), code -> true, (raw, stored) -> false, noOtp(), rejectingSso(), tokenIssuer(), attempts);
        assertThatThrownBy(() -> wrongPassword.login(normal("admin", null)))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(attempts.lastFailureReason()).isEqualTo("INVALID_PASSWORD");

        AuthService missingUser = service(
                users(Map.of()), code -> true, validPasswordVerifier(), noOtp(), rejectingSso(), tokenIssuer(), attempts);
        assertThatThrownBy(() -> missingUser.login(normal("missing", null)))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(attempts.lastFailureReason()).isEqualTo("USER_NOT_FOUND");
    }

    @Test
    void rejectsInactiveLockedInvalidDepartmentAndMissingRoles() {
        assertThatThrownBy(() -> serviceForUser(user(false, false, "FIN", approvedRoles()))
                        .login(normal("admin", null)))
                .isInstanceOf(UserAccessDeniedException.class)
                .hasMessageContaining("inactive");
        assertThatThrownBy(() -> serviceForUser(user(true, true, "FIN", approvedRoles()))
                        .login(normal("admin", null)))
                .isInstanceOf(UserAccessDeniedException.class)
                .hasMessageContaining("locked");

        AuthService invalidDepartment = service(
                users(Map.of("admin", user(true, false, "UNKNOWN", approvedRoles()))),
                code -> false, validPasswordVerifier(), noOtp(), rejectingSso(), tokenIssuer(),
                new RecordingLoginAttemptPort());
        assertThatThrownBy(() -> invalidDepartment.login(normal("admin", null)))
                .isInstanceOf(UserAccessDeniedException.class)
                .hasMessageContaining("Department code is invalid");

        RoleAssignment expired = new RoleAssignment("ROLE_INTERNAL", "GLOBAL", null, AUTHENTICATED_AT, true);
        assertThatThrownBy(() -> serviceForUser(user(true, false, "FIN", List.of(expired)))
                        .login(normal("admin", null)))
                .isInstanceOf(UserAccessDeniedException.class)
                .hasMessageContaining("no approved effective roles");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "KERBEROS"})
    void rejectsUnsupportedLoginTypesBeforeAttemptOrCredentialAdapters(String loginType) {
        AtomicBoolean userQueried = new AtomicBoolean();
        RecordingLoginAttemptPort attempts = new RecordingLoginAttemptPort();
        AuthService service = service(
                new AuthUserQueryPort() {
                    @Override
                    public Optional<AuthUser> findByUsername(String username) {
                        userQueried.set(true);
                        return Optional.empty();
                    }

                    @Override
                    public List<AuthUser> findAllUsers() {
                        userQueried.set(true);
                        return List.of();
                    }
                },
                code -> true,
                validPasswordVerifier(),
                noOtp(),
                rejectingSso(),
                tokenIssuer(),
                attempts);

        assertThatThrownBy(() -> service.login(
                        new AuthUseCase.LoginCommand("admin", validPassword, loginType, null, null)))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(userQueried).isFalse();
        assertThat(attempts.isLockedCalls).isZero();
    }

    @Test
    void validateTokenVersionRequiresMatchingVersionAndAvailableAccount() {
        RoleAssignment expired = new RoleAssignment("ROLE_INTERNAL", "GLOBAL", null, AUTHENTICATED_AT, true);
        Map<String, AuthUser> userMap = Map.of(
                "active", namedUser("active", true, false, approvedRoles()),
                "inactive", namedUser("inactive", false, false, approvedRoles()),
                "locked", namedUser("locked", true, true, approvedRoles()),
                "expired", namedUser("expired", true, false, List.of(expired)));
        AuthService service = service(
                users(userMap), code -> true, validPasswordVerifier(), noOtp(), rejectingSso(), tokenIssuer(),
                new RecordingLoginAttemptPort());

        assertThat(service.validateTokenVersion("active", 1L)).isTrue();
        assertThat(service.validateTokenVersion("active", 0L)).isFalse();
        assertThat(service.validateTokenVersion("active", 2L)).isFalse();
        assertThat(service.validateTokenVersion("inactive", 1L)).isFalse();
        assertThat(service.validateTokenVersion("locked", 1L)).isFalse();
        assertThat(service.validateTokenVersion("expired", 1L)).isFalse();
        assertThat(service.validateTokenVersion("missing", 1L)).isFalse();
    }

    @Test
    void existingScopedSystemAdministratorCannotLoginOrValidatePreviouslyIssuedToken() {
        RoleAssignment scopedAdmin = new RoleAssignment(
                "ROLE_SYSTEM_ADMIN", "FIN", null, null, true);
        RecordingLoginAttemptPort attempts = new RecordingLoginAttemptPort();
        AtomicBoolean tokenIssued = new AtomicBoolean();
        AuthService service = service(
                users(Map.of("admin", user(true, false, "FIN", List.of(scopedAdmin)))),
                code -> true, validPasswordVerifier(), noOtp(), rejectingSso(),
                (subject, issuedAt) -> {
                    tokenIssued.set(true);
                    return new TokenIssuerPort.IssuedToken("token", 1L);
                }, attempts);

        assertThatThrownBy(() -> service.login(normal("admin", null)))
                .isInstanceOf(UserAccessDeniedException.class);
        assertThat(tokenIssued).isFalse();
        assertThat(attempts.successes).isEmpty();
        assertThat(service.validateTokenVersion("admin", 1L)).isFalse();
    }

    @Test
    void globalSystemAdministratorCanLoginAndValidateCurrentTokenVersion() {
        AuthService service = serviceForUser(user(true, false, "FIN", List.of(
                new RoleAssignment("ROLE_SYSTEM_ADMIN", "GLOBAL", null, null, true))));

        assertThat(service.login(normal("admin", null)).roles()).containsExactly("ROLE_SYSTEM_ADMIN");
        assertThat(service.validateTokenVersion("admin", 1L)).isTrue();
    }

    @Test
    void globalRoleCannotMaskAnotherEffectiveLegacyScopedAssignment() {
        AuthService service = serviceForUser(user(true, false, "FIN", List.of(
                RoleAssignment.approved("ROLE_SYSTEM_ADMIN"),
                new RoleAssignment("ROLE_AUDITOR", "FIN", null, null, true))));

        assertThatThrownBy(() -> service.login(normal("admin", null)))
                .isInstanceOf(UserAccessDeniedException.class);
        assertThat(service.validateTokenVersion("admin", 1L)).isFalse();
    }

    @Test
    void expiredLegacyScopedRoleStillInvalidatesOldTokenDespiteEffectiveGlobalRole() {
        AuthService service = serviceForUser(user(true, false, "FIN", List.of(
                RoleAssignment.approved("ROLE_USER"),
                new RoleAssignment("ROLE_MASTER_MANAGER", "FIN", null,
                        AUTHENTICATED_AT.minusSeconds(1), true))));

        assertThatThrownBy(() -> service.login(normal("admin", null)))
                .isInstanceOf(UserAccessDeniedException.class);
        assertThat(service.validateTokenVersion("admin", 1L)).isFalse();
    }

    private AuthService standardService(
            OtpVerificationPort otp, SsoAuthenticationPort sso, LoginAttemptPort attempts) {
        return service(standardUsers(), code -> true, validPasswordVerifier(), otp, sso, tokenIssuer(), attempts);
    }

    private AuthService serviceForUser(AuthUser user) {
        return service(
                users(Map.of("admin", user)), code -> true, validPasswordVerifier(), noOtp(), rejectingSso(),
                tokenIssuer(), new RecordingLoginAttemptPort());
    }

    private AuthService service(
            AuthUserQueryPort users,
            DepartmentValidationPort departments,
            PasswordVerifierPort passwords,
            OtpVerificationPort otp,
            SsoAuthenticationPort sso,
            TokenIssuerPort tokens,
            LoginAttemptPort attempts) {
        return new AuthService(users, departments, passwords, otp, sso, tokens, attempts, CLOCK);
    }

    private AuthUseCase.LoginCommand normal(String username, String otp) {
        return new AuthUseCase.LoginCommand(username, validPassword, "NORMAL", otp, null);
    }

    private AuthUseCase.LoginCommand ldap(String otp) {
        return new AuthUseCase.LoginCommand("admin", validPassword, "LDAP", otp, null);
    }

    private AuthUseCase.LoginCommand sso(String username, String credential) {
        return new AuthUseCase.LoginCommand(username, credential, "SSO", null, "corporate-oidc");
    }

    private AuthUserQueryPort standardUsers() {
        return users(Map.of("admin", user(true, false, "FIN", approvedRoles())));
    }

    private AuthUserQueryPort users(Map<String, AuthUser> users) {
        return new AuthUserQueryPort() {
            @Override
            public Optional<AuthUser> findByUsername(String username) {
                return Optional.ofNullable(users.get(username));
            }

            @Override
            public List<AuthUser> findAllUsers() {
                return List.copyOf(users.values());
            }
        };
    }

    private AuthUser user(boolean active, boolean locked, String department, List<RoleAssignment> assignments) {
        return namedUser("admin", active, locked, department, assignments);
    }

    private AuthUser namedUser(String username, boolean active, boolean locked, List<RoleAssignment> assignments) {
        return namedUser(username, active, locked, "FIN", assignments);
    }

    private AuthUser namedUser(
            String username, boolean active, boolean locked, String department, List<RoleAssignment> assignments) {
        return new AuthUser(username, "stored", department, active, locked, assignments, 1L);
    }

    private List<RoleAssignment> approvedRoles() {
        return List.of(RoleAssignment.approved("ROLE_INTERNAL"));
    }

    private PasswordVerifierPort validPasswordVerifier() {
        return (raw, stored) -> validPassword.equals(raw);
    }

    private OtpVerificationPort noOtp() {
        return new OtpVerificationPort() {
            @Override
            public boolean verifyOtp(String username, String otpCode) {
                return false;
            }

            @Override
            public boolean requiresOtp(String username) {
                return false;
            }
        };
    }

    private SsoAuthenticationPort profile(String subject, List<String> providerRoles) {
        return (provider, credential) -> new SsoUserProfile(
                subject, subject + "@example.test", subject, "FIN", providerRoles);
    }

    private SsoAuthenticationPort rejectingSso() {
        return (provider, credential) -> { throw new SsoAuthenticationPort.SsoAuthenticationException(); };
    }

    private TokenIssuerPort tokenIssuer() {
        return (subject, issuedAt) -> new TokenIssuerPort.IssuedToken("token", 1L);
    }

    private InMemoryLoginAttemptAdapter inMemoryAttempts(int maxFailures) {
        AuthModuleProperties properties = new AuthModuleProperties();
        properties.getLoginSecurity().setMaxFailures(maxFailures);
        properties.getLoginSecurity().setLockDurationMinutes(15);
        return new InMemoryLoginAttemptAdapter(properties, CLOCK);
    }

    private static final class RecordingOtpPort implements OtpVerificationPort {
        private final boolean required;
        private final String validCode;
        private String lastUsername;
        private String lastCode;

        private RecordingOtpPort(boolean required, String validCode) {
            this.required = required;
            this.validCode = validCode;
        }

        @Override
        public boolean verifyOtp(String username, String otpCode) {
            lastUsername = username;
            lastCode = otpCode;
            return validCode.equals(otpCode);
        }

        @Override
        public boolean requiresOtp(String username) {
            return required;
        }
    }

    private static final class RecordingLoginAttemptPort implements LoginAttemptPort {
        private final List<String> failures = new ArrayList<>();
        private final List<String> successes = new ArrayList<>();
        private int isLockedCalls;

        @Override
        public boolean isLocked(String username) {
            isLockedCalls++;
            return false;
        }

        @Override
        public void recordFailure(String username, String reason) {
            failures.add(username + ":" + reason);
        }

        @Override
        public void recordSuccess(String username) {
            successes.add(username);
        }

        private String lastFailureReason() {
            String failure = failures.get(failures.size() - 1);
            return failure.substring(failure.indexOf(':') + 1);
        }
    }
}
