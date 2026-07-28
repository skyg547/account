package com.ho.account.gateway.security;

/**
 * Auth의 현재 권한 버전과 JWT 스냅샷을 비교한 결과입니다.
 *
 * <p>{@code REJECTED}는 다시 로그인해야 하는 업무 결과이고, {@code UNAVAILABLE}은 Auth 장애라서
 * 재로그인으로 해결되지 않는 운영 결과입니다. 둘을 나눠야 HTTP 401과 503을 정확히 선택할 수 있습니다.</p>
 */
public enum TokenVersionValidationResult {
    VALID,
    REJECTED,
    UNAVAILABLE
}
