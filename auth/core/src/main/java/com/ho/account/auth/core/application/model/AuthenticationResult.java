package com.ho.account.auth.core.application.model;

import java.util.List;

/**
 * 로그인 유즈케이스가 API 어댑터에 전달하는 기술 중립적인 인증 결과입니다.
 *
 * <p>HTTP 필드명이나 응답 상태는 API 계층이 결정하고, core는 인증 시점에 확정된
 * 사용자·역할·토큰 정보만 반환합니다.</p>
 */
public record AuthenticationResult(
        String accessToken,
        long expiresInSeconds,
        String username,
        String departmentCode,
        List<String> roles,
        long roleVersion) {

    public AuthenticationResult {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
