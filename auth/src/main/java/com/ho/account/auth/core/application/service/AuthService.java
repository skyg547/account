package com.ho.account.auth.core.application.service;

import com.ho.account.auth.api.dto.LoginResponse;
import com.ho.account.auth.core.application.exception.InvalidCredentialsException;
import com.ho.account.auth.core.application.exception.UserAccessDeniedException;
import com.ho.account.auth.core.application.port.in.AuthUseCase;
import com.ho.account.auth.core.application.port.out.AuthUserQueryPort;
import com.ho.account.auth.core.application.port.out.DepartmentValidationPort;
import com.ho.account.auth.core.application.port.out.PasswordVerifierPort;
import com.ho.account.auth.core.application.port.out.TokenIssuerPort;
import com.ho.account.auth.core.domain.model.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [애플리케이션 서비스] AuthService
 * 인증(Authentication) 및 인가(Authorization) 유즈케이스를 처리하는 핵심 서비스입니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 서비스는 건물 입구의 '보안 요원'과 같습니다.
 * 누군가 건물(시스템)에 들어오려고 할 때, 
 * 1) 신분증에 적힌 이름이 명단에 있는지 확인하고(사용자 조회), 
 * 2) 비밀번호가 맞는지 검사하며(비밀번호 검증), 
 * 3) 퇴사자이거나 정직 중인 사람은 아닌지(계정 활성화 및 잠금 확인), 
 * 4) 소속 부서가 실제로 존재하는지(부서 검증) 꼼꼼하게 따집니다.
 * 모든 검사를 무사히 통과하면 건물 출입증(JWT 토큰)을 발급해 줍니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService implements AuthUseCase {

    private final AuthUserQueryPort authUserQueryPort;
    private final DepartmentValidationPort departmentValidationPort;
    private final PasswordVerifierPort passwordVerifierPort;
    private final TokenIssuerPort tokenIssuerPort;

    @Override
    public LoginResponse login(String username, String password) {
        AuthUser user = authUserQueryPort.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordVerifierPort.matches(password, user.getStoredPassword())) {
            throw new InvalidCredentialsException();
        }

        if (!user.isActive()) {
            throw new UserAccessDeniedException("User account is inactive");
        }

        if (user.isLocked()) {
            throw new UserAccessDeniedException("User account is locked");
        }

        if (user.getDepartmentCode() != null && !user.getDepartmentCode().isBlank()
                && !departmentValidationPort.existsDepartmentCode(user.getDepartmentCode())) {
            throw new UserAccessDeniedException("Department code is invalid: " + user.getDepartmentCode());
        }

        if (user.getRoles().isEmpty()) {
            throw new UserAccessDeniedException("User has no approved effective roles");
        }

        TokenIssuerPort.IssuedToken issuedToken = tokenIssuerPort.issue(user);
        return new LoginResponse(
                issuedToken.token(),
                "Bearer",
                issuedToken.expiresInSeconds(),
                user.getUsername(),
                user.getDepartmentCode(),
                user.getRoles(),
                user.getRoleVersion());
    }
}
