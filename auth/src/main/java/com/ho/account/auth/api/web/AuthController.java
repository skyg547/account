package com.ho.account.auth.api.web;

import com.ho.account.auth.api.dto.LoginRequest;
import com.ho.account.auth.api.dto.LoginResponse;
import com.ho.account.auth.api.dto.RoleAssignmentApplyRequest;
import com.ho.account.auth.api.dto.RoleAssignmentApplyResponse;
import com.ho.account.auth.api.dto.TokenValidationRequest;
import com.ho.account.auth.api.dto.TokenValidationResponse;
import com.ho.account.auth.core.application.port.in.AuthUseCase;
import com.ho.account.auth.core.application.port.in.AuthUserRoleAssignmentUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;
    private final AuthUserRoleAssignmentUseCase authUserRoleAssignmentUseCase;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authUseCase.login(request.username(), request.password());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate-token-version")
    public ResponseEntity<TokenValidationResponse> validateTokenVersion(
            @Valid @RequestBody TokenValidationRequest request) {
        boolean valid = authUseCase.validateTokenVersion(request.username(), request.roleVersion());
        return ResponseEntity.ok(new TokenValidationResponse(
                valid, valid ? "OK" : "Token version is outdated due to role changes. Please re-login."));
    }

    @PostMapping("/internal/users/{username}/role-assignments")
    public ResponseEntity<RoleAssignmentApplyResponse> replaceRoleAssignments(
            @PathVariable String username,
            @Valid @RequestBody RoleAssignmentApplyRequest request) {
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
}
