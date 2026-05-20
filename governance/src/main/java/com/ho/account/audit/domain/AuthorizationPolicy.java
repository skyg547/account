package com.ho.account.audit.domain;

import java.util.List;
import java.util.Objects;

/**
 * RBAC 권한 부여와 조회 시 적용하는 내부통제 정책.
 */
public class AuthorizationPolicy {

    public void validateGrant(
            SystemRole role,
            List<Authorization> existingAuthorizations,
            String functionCode,
            AccessType accessType,
            String dataScope) {
        Objects.requireNonNull(role, "role must not be null");
        String normalizedFunction = requireText(functionCode, "Function code is required.");
        Objects.requireNonNull(accessType, "accessType must not be null");
        normalizeDataScope(dataScope);

        if (hasSodConflict(existingAuthorizations, normalizedFunction, accessType)) {
            throw new IllegalStateException("SOD conflict: WRITE and EXECUTE cannot be granted together for function group "
                    + functionGroup(normalizedFunction));
        }
    }

    public AuthorizationDecision decide(
            List<Authorization> authorizations,
            String functionCode,
            AccessType accessType) {
        String normalizedFunction = requireText(functionCode, "Function code is required.");
        Objects.requireNonNull(accessType, "accessType must not be null");

        return safe(authorizations).stream()
                .filter(authorization -> normalizedFunction.equals(authorization.getFunctionCode()))
                .filter(authorization -> accessType == authorization.getAccessType())
                .findFirst()
                .map(authorization -> AuthorizationDecision.granted(authorization.getDataScope()))
                .orElseGet(() -> AuthorizationDecision.denied("NO_MATCHING_GRANT"));
    }

    public String normalizeDataScope(String dataScope) {
        return dataScope == null || dataScope.isBlank() ? "GLOBAL" : dataScope.trim();
    }

    private boolean hasSodConflict(List<Authorization> existingAuthorizations, String functionCode, AccessType accessType) {
        if (accessType != AccessType.WRITE && accessType != AccessType.EXECUTE) {
            return false;
        }

        String group = functionGroup(functionCode);
        AccessType conflictingType = accessType == AccessType.WRITE ? AccessType.EXECUTE : AccessType.WRITE;
        return safe(existingAuthorizations).stream()
                .filter(authorization -> authorization.getFunctionCode() != null)
                .anyMatch(authorization -> group.equals(functionGroup(authorization.getFunctionCode()))
                        && conflictingType == authorization.getAccessType());
    }

    private String functionGroup(String functionCode) {
        int dot = functionCode.indexOf('.');
        int colon = functionCode.indexOf(':');
        int separator = dot >= 0 && colon >= 0 ? Math.min(dot, colon) : Math.max(dot, colon);
        return separator > 0 ? functionCode.substring(0, separator) : functionCode;
    }

    private List<Authorization> safe(List<Authorization> authorizations) {
        return authorizations == null ? List.of() : authorizations;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
