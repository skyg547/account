package com.ho.account.auth.api.web;

import com.ho.account.auth.api.dto.LoginRequest;
import com.ho.account.auth.api.dto.LoginResponse;
import com.ho.account.auth.api.dto.RoleAssignmentApplyRequest;
import com.ho.account.auth.api.dto.RoleAssignmentApplyResponse;
import com.ho.account.auth.api.dto.TokenValidationRequest;
import com.ho.account.auth.api.dto.TokenValidationResponse;
import com.ho.account.auth.core.application.exception.UserAccessDeniedException;
import com.ho.account.auth.core.application.model.AuthenticationResult;
import com.ho.account.auth.core.application.port.in.AuthUseCase;
import com.ho.account.auth.core.application.port.in.AuthUserRoleAssignmentUseCase;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String INTERNAL_AUTH_TOKEN_HEADER = "X-Internal-Auth-Token";

    private final AuthUseCase authUseCase;
    private final AuthUserRoleAssignmentUseCase authUserRoleAssignmentUseCase;
    private final AuthModuleProperties authModuleProperties;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthenticationResult result = authUseCase.login(
                new AuthUseCase.LoginCommand(request.username(), request.password()));
        return ResponseEntity.ok(LoginResponse.from(result));
    }

    @PostMapping("/validate-token-version")
    public ResponseEntity<TokenValidationResponse> validateTokenVersion(
            @Valid @RequestBody TokenValidationRequest request) {
        boolean valid = authUseCase.validateTokenVersion(request.username(), request.roleVersion());
        return ResponseEntity.ok(new TokenValidationResponse(
                valid, valid ? "OK" : "Token version is outdated or the account is unavailable. Please re-login."));
    }

    @PostMapping("/internal/users/{username}/role-assignments")
    public ResponseEntity<RoleAssignmentApplyResponse> replaceRoleAssignments(
            @PathVariable String username,
            @RequestHeader(value = INTERNAL_AUTH_TOKEN_HEADER, required = false) String internalAuthToken,
            @Valid @RequestBody RoleAssignmentApplyRequest request) {
        verifyInternalToken(internalAuthToken);
        AuthUserRoleAssignmentUseCase.RoleAssignmentResult result =
                authUserRoleAssignmentUseCase.replaceRoleAssignments(
                        new AuthUserRoleAssignmentUseCase.ReplaceRoleAssignmentsCommand(
                                username,
                                request.roleCodes(),
                                request.dataScope(),
                                request.validFrom(),
                                request.validTo(),
                                request.approvedBy(),
                                request.approvalTraceId()));
        return ResponseEntity.ok(new RoleAssignmentApplyResponse(
                result.username(),
                result.roleVersion(),
                result.roles()));
    }

    private void verifyInternalToken(String providedToken) {
        String expectedToken = authModuleProperties.getInternalApi().getToken();
        if (!StringUtils.hasText(expectedToken)
                || !StringUtils.hasText(providedToken)
                || !MessageDigest.isEqual(
                        expectedToken.getBytes(StandardCharsets.UTF_8),
                        providedToken.getBytes(StandardCharsets.UTF_8))) {
            throw new UserAccessDeniedException("Invalid internal auth token");
        }
    }
}
