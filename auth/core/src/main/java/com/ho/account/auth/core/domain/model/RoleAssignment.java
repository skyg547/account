package com.ho.account.auth.core.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Auth 사용자에게 승인된 역할 부여 상태를 표현하는 값 객체입니다.
 *
 * <p>유효기간은 `[validFrom, validTo)` 반개구간입니다. 시작 시각은 포함하고 종료 시각은
 * 포함하지 않아 인접한 두 역할 기간이 같은 순간에 중복 적용되지 않게 합니다.</p>
 */
public record RoleAssignment(
        String roleCode,
        String dataScope,
        Instant validFrom,
        Instant validTo,
        boolean approved) {

    public RoleAssignment {
        if (roleCode == null || roleCode.isBlank()) {
            throw new IllegalArgumentException("roleCode is required.");
        }
        roleCode = roleCode.trim();
        if (roleCode.length() > 80) {
            throw new IllegalArgumentException("roleCode must be at most 80 characters.");
        }
        // Keep even missing legacy scope text intact so authorization can deny it without a mapping error.
        if (dataScope != null && dataScope.length() > 80) {
            throw new IllegalArgumentException("dataScope must be at most 80 characters.");
        }
        if (validFrom != null && validTo != null && !validFrom.isBefore(validTo)) {
            throw new IllegalArgumentException("validFrom must be before validTo.");
        }
    }

    public static RoleAssignment approved(String roleCode) {
        return new RoleAssignment(roleCode, "GLOBAL", null, null, true);
    }

    public boolean hasSupportedAuthorizationScope() {
        return "GLOBAL".equals(dataScope);
    }

    public boolean isEffectiveAt(Instant evaluatedAt) {
        Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        boolean afterStart = validFrom == null || !evaluatedAt.isBefore(validFrom);
        boolean beforeEnd = validTo == null || evaluatedAt.isBefore(validTo);
        return approved && afterStart && beforeEnd;
    }
}
