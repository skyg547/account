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
import org.springframework.web.bind.annotation.*;

/**
 * 🐣 [초보자를 위한 AuthController 개념 가이드]
 * 
 * 1. CORS (Cross-Origin Resource Sharing) 해결:
 *    - 웹 브라우저(Next.js: localhost:3000, 3001)에서 다른 도메인/포트의 백엔드(localhost:8080)로
 *      API 요청을 보낼 때 보안상 브라우저가 교차 출처 요청을 차단하는 것이 'CORS 에러'입니다.
 *    - `@CrossOrigin(origins = "*", allowedHeaders = "*")` 어노테이션을 추가하여
 *      프론트엔드 브라우저의 접근 요청을 허용합니다.
 * 
 * 2. 다중 권한(Roles)과 JWT 토큰:
 *    - 사용자는 "지출 결의 승인자(ROLE_EXPENDITURE_APPROVER)", "거래처 관리자(ROLE_PARTNER_MANAGER)" 등
 *      여러 개의 권한을 동시에 가질 수 있습니다.
 *    - JWT 토큰 내부(Payload)는 JSON 구조이므로 `roles: ["ROLE_ADMIN", "ROLE_EXPENDITURE_APPROVER"]`처럼
 *      배열(List) 형태로 사용자의 모든 권한 목록을 실어서 발급합니다.
 */
@CrossOrigin(origins = "*", allowedHeaders = "*")
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
