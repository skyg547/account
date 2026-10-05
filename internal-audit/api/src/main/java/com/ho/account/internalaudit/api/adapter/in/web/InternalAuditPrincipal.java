package com.ho.account.internalaudit.api.adapter.in.web;

import java.util.List;
import java.util.Locale;

/** Identity derived only from a locally verified access token and current Auth role version. */
public record InternalAuditPrincipal(String username, List<String> roles, long roleVersion) {
    public InternalAuditPrincipal {
        username = headerSafeCode(username, 128);
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("Invalid token roles");
        }
        if (roleVersion < 1) {
            throw new IllegalArgumentException("Invalid role version");
        }
        // Auth may sign AUDITOR or ROLE_AUDITOR; both name the same configured function grants.
        roles = roles.stream().map(role -> headerSafeCode(role, 80).toUpperCase(Locale.ROOT))
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .distinct().toList();
    }

    private static String headerSafeCode(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Invalid identity claim");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException("Invalid identity claim length");
        }
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (current < 0x21 || current > 0x7e || current == ',') {
                throw new IllegalArgumentException("Unsafe identity claim");
            }
        }
        return normalized;
    }
}
