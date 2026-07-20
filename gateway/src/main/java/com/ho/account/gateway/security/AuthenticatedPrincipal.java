package com.ho.account.gateway.security;

import java.util.List;

/**
 * 서명이 검증된 JWT에서 꺼낸 내부 사용자 신원입니다.
 *
 * <p>초보자 설명: 외부 클라이언트가 보낸 {@code X-Auth-*} 헤더는 믿지 않습니다. Gateway가 JWT를
 * 검증한 뒤 이 객체를 만들고, 여기에서 다시 생성한 헤더만 뒤쪽 서비스에 전달합니다.</p>
 */
public record AuthenticatedPrincipal(
        String username,
        List<String> roles,
        long roleVersion,
        String departmentCode) {

    private static final int MAX_USERNAME_LENGTH = 128;
    private static final int MAX_ROLE_LENGTH = 80;
    private static final int MAX_DEPARTMENT_LENGTH = 100;

    public AuthenticatedPrincipal {
        username = requireHeaderSafeCode(username, "username", MAX_USERNAME_LENGTH);
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("roles must not be empty");
        }
        roles = roles.stream()
                .map(role -> requireHeaderSafeCode(role, "role", MAX_ROLE_LENGTH))
                .distinct()
                .toList();
        if (roleVersion < 1L) {
            throw new IllegalArgumentException("roleVersion must be 1 or greater");
        }
        departmentCode = normalizeOptionalHeaderSafeCode(
                departmentCode,
                "departmentCode",
                MAX_DEPARTMENT_LENGTH);
    }

    private static String normalizeOptionalHeaderSafeCode(String value, String fieldName, int maxLength) {
        return value == null || value.isBlank()
                ? null
                : requireHeaderSafeCode(value, fieldName, maxLength);
    }

    private static String requireHeaderSafeCode(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " is too long");
        }
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (current < 0x21 || current > 0x7e || current == ',') {
                throw new IllegalArgumentException(fieldName + " contains a character that is unsafe for an HTTP header");
            }
        }
        return normalized;
    }
}
