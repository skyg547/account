package com.ho.account.internalaudit.api.adapter.in.web;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Exact module grants for the two API functions. The shared-kernel permission endpoint is not
 * hosted by this service, so this matrix must be provisioned and revoked in module configuration.
 * Missing grants always deny access.
 */
@Component
final class InternalAuditPermissionPolicy {
    private static final Set<String> AUDIT_ROLES = Set.of("ROLE_AUDITOR", "ROLE_ADMIN");
    private final Set<Grant> grants;

    InternalAuditPermissionPolicy(@Value("${internal-audit.identity.permission-grants:}") String configuredGrants) {
        this.grants = Arrays.stream(configuredGrants.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(InternalAuditPermissionPolicy::parseGrant)
                .collect(Collectors.toUnmodifiableSet());
    }

    boolean hasPermission(InternalAuditPrincipal principal, String function, String access) {
        return principal.roles().stream()
                .filter(AUDIT_ROLES::contains)
                .anyMatch(role -> grants.contains(new Grant(role, function, access)));
    }

    private static Grant parseGrant(String value) {
        String[] parts = value.split(":", -1);
        if (parts.length != 3 || Arrays.stream(parts).anyMatch(String::isBlank)
                || !Set.of("INTERNAL_AUDIT.RCM", "INTERNAL_AUDIT.EVALUATION").contains(parts[1])
                || !Set.of("READ", "WRITE").contains(parts[2])) {
            throw new IllegalArgumentException("Invalid internal-audit identity permission grant");
        }
        return new Grant(parts[0], parts[1], parts[2]);
    }

    private record Grant(String role, String function, String access) {
    }
}
