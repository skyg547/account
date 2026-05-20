package com.ho.account.auth.core.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Auth 사용자에게 승인된 역할 부여 상태를 표현하는 값 객체.
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
        dataScope = dataScope == null || dataScope.isBlank() ? "GLOBAL" : dataScope.trim();
        if (validFrom != null && validTo != null && validFrom.isAfter(validTo)) {
            throw new IllegalArgumentException("validFrom must be before validTo.");
        }
    }

    public static RoleAssignment approved(String roleCode) {
        return new RoleAssignment(roleCode, "GLOBAL", null, null, true);
    }

    public boolean isEffectiveAt(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        boolean afterStart = validFrom == null || !now.isBefore(validFrom);
        boolean beforeEnd = validTo == null || now.isBefore(validTo);
        return approved && afterStart && beforeEnd;
    }
}
