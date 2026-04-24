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

        TokenIssuerPort.IssuedToken issuedToken = tokenIssuerPort.issue(user);
        return new LoginResponse(
                issuedToken.token(),
                "Bearer",
                issuedToken.expiresInSeconds(),
                user.getUsername(),
                user.getDepartmentCode(),
                user.getRoles());
    }
}
