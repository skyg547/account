package com.ho.account.auth.core.application.service;

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
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * [애플리케이션 서비스] AuthService
 * 인증(Authentication) 및 인가(Authorization) 유즈케이스를 처리하는 핵심 서비스입니다.
 *
 * <p>🐣 [초보자를 위한 설명]
 * 이 서비스는 건물 입구의 '보안 요원'과 같습니다.
 * 누군가 건물(시스템)에 들어오려고 할 때,
 * 1) 신분증에 적힌 이름이 명단에 있는지 확인하고(사용자 조회),
 * 2) 비밀번호가 맞는지 검사하며(비밀번호 검증),
 * 3) 퇴사자이거나 정직 중인 사람은 아닌지(계정 활성화 및 잠금 확인),
 * 4) 소속 부서가 실제로 존재하는지(부서 검증) 꼼꼼하게 따집니다.
 * 모든 검사를 무사히 통과하면 같은 시점의 역할 목록으로 JWT와 로그인 결과를 만듭니다.</p>
 */
@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final AuthUserQueryPort authUserQueryPort;
    private final DepartmentValidationPort departmentValidationPort;
    private final PasswordVerifierPort passwordVerifierPort;
    private final TokenIssuerPort tokenIssuerPort;
    private final LoginAttemptPort loginAttemptPort;
    private final Clock clock;

    // 사용자 조회, 원격 부서 확인, 실패 기록을 하나의 DB 트랜잭션으로 묶지 않습니다.
    // 각 출력 어댑터가 짧은 read/write 트랜잭션을 소유해 잠금 기록이 readOnly에 묻히지 않게 합니다.
    @Override
    public AuthenticationResult login(LoginCommand command) {
        if (command == null || command.username() == null || command.username().isBlank()) {
            throw new InvalidCredentialsException();
        }
        
        boolean isSso = "SSO".equalsIgnoreCase(command.loginType());
        boolean isLdap = "LDAP".equalsIgnoreCase(command.loginType());

        if (!isSso) {
            if (command.password() == null || command.password().isBlank()) {
                throw new InvalidCredentialsException();
            }
        }

        String username = command.username().trim();

        // 먼저 임시 잠금을 확인하여 반복 대입 공격이 사용자 조회/비밀번호 검증까지 도달하지 않게 합니다.
        if (loginAttemptPort.isLocked(username)) {
            throw new UserAccessDeniedException("User account is temporarily locked after repeated login failures");
        }

        AuthUser user = authUserQueryPort.findByUsername(username)
                .orElseThrow(() -> {
                    loginAttemptPort.recordFailure(username, "USER_NOT_FOUND");
                    return new InvalidCredentialsException();
                });

        if (!isSso) {
            if (!passwordVerifierPort.matches(command.password(), user.getStoredPassword())) {
                loginAttemptPort.recordFailure(username, "INVALID_PASSWORD");
                throw new InvalidCredentialsException();
            }

            if (isLdap) {
                // LDAP OTP 인증 공급자가 연결되기 전에는 어떤 OTP도 신뢰하지 않습니다.
                loginAttemptPort.recordFailure(username, "LDAP_OTP_VERIFIER_UNAVAILABLE");
                throw new InvalidCredentialsException();
            }
        }

        if (!user.isActive()) {
            loginAttemptPort.recordFailure(username, "INACTIVE_ACCOUNT");
            throw new UserAccessDeniedException("User account is inactive");
        }

        if (user.isLocked()) {
            loginAttemptPort.recordFailure(username, "ADMINISTRATIVELY_LOCKED");
            throw new UserAccessDeniedException("User account is locked");
        }

        if (user.getDepartmentCode() != null && !user.getDepartmentCode().isBlank()
                && !departmentValidationPort.existsDepartmentCode(user.getDepartmentCode())) {
            loginAttemptPort.recordFailure(username, "INVALID_DEPARTMENT");
            throw new UserAccessDeniedException("Department code is invalid: " + user.getDepartmentCode());
        }

        Instant authenticatedAt = clock.instant();
        List<RoleAssignment> effectiveAssignments = user.effectiveRoleAssignmentsAt(authenticatedAt);
        if (effectiveAssignments.isEmpty()) {
            loginAttemptPort.recordFailure(username, "NO_EFFECTIVE_ROLE");
            throw new UserAccessDeniedException("User has no approved effective roles");
        }

        List<String> roles = effectiveAssignments.stream()
                .map(RoleAssignment::roleCode)
                .distinct()
                .toList();
        TokenIssuerPort.IssuedToken issuedToken = tokenIssuerPort.issue(
                new TokenIssuerPort.TokenSubject(
                        user.getUsername(),
                        user.getDepartmentCode(),
                        effectiveAssignments,
                        user.getRoleVersion()),
                authenticatedAt);
        loginAttemptPort.recordSuccess(username);

        return new AuthenticationResult(
                issuedToken.token(),
                issuedToken.expiresInSeconds(),
                user.getUsername(),
                user.getDepartmentCode(),
                roles,
                user.getRoleVersion());
    }

    @Override
    public boolean validateTokenVersion(String username, long roleVersion) {
        if (username == null || username.isBlank() || roleVersion < 1) {
            return false;
        }
        Instant validatedAt = clock.instant();
        return authUserQueryPort.findByUsername(username.trim())
                .filter(AuthUser::isActive)
                .filter(user -> !user.isLocked())
                .filter(user -> !user.effectiveRoleAssignmentsAt(validatedAt).isEmpty())
                .map(user -> user.getRoleVersion() == roleVersion)
                .orElse(false);
    }
}
