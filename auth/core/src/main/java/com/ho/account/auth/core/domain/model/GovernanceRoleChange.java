package com.ho.account.auth.core.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import static com.ho.account.auth.core.domain.model.GovernanceChangePolicy.*;

/** A complete GLOBAL replacement for one exact Auth identity, effective from midnight UTC. */
public record GovernanceRoleChange(String username, List<String> roleCodes, String dataScope,
        Instant validFrom, Instant validTo) {
    public GovernanceRoleChange {
        text(username, AuthIdentityPolicy.MAX_USERNAME_LENGTH);
        require(roleCodes != null && !roleCodes.isEmpty() && roleCodes.size() <= 100);
        roleCodes = roleCodes.stream().map(GovernanceRoleChange::normalizeRole).distinct().sorted().toList();
        dataScope = scope(dataScope);
        require(validFrom != null);
        for (String role : roleCodes) new RoleAssignment(role, dataScope, validFrom, validTo, true);
    }

    public static GovernanceRoleChange forTarget(String masterKey, String username, List<String> roles,
            String dataScope, LocalDate effectiveDate, Instant validTo) {
        require(text(masterKey, AuthIdentityPolicy.MAX_USERNAME_LENGTH).equals(username));
        require(effectiveDate != null);
        return new GovernanceRoleChange(username, roles, dataScope,
                effectiveDate.atStartOfDay(ZoneOffset.UTC).toInstant(), validTo);
    }

    private static String normalizeRole(String value) {
        text(value, 80);
        String role = value.toUpperCase(Locale.ROOT);
        if (!role.startsWith("ROLE_")) role = "ROLE_" + role;
        require(role.matches("ROLE_[A-Z0-9_]+") && role.length() <= 80);
        return role;
    }
}
