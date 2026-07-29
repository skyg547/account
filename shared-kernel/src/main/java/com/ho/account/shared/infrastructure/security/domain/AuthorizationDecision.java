package com.ho.account.shared.infrastructure.security.domain;

/**
 * 권한 판단 결과를 이유와 함께 전달하는 도메인 값 객체.
 */
public record AuthorizationDecision(
        boolean granted,
        String reason,
        String dataScope) {

    public static AuthorizationDecision granted(String dataScope) {
        return new AuthorizationDecision(true, "GRANTED", normalizeScope(dataScope));
    }

    public static AuthorizationDecision denied(String reason) {
        return new AuthorizationDecision(false, reason, "NONE");
    }

    private static String normalizeScope(String dataScope) {
        return dataScope == null || dataScope.isBlank() ? "GLOBAL" : dataScope.trim();
    }
}
