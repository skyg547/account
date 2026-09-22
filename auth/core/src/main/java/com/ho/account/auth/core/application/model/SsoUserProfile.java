package com.ho.account.auth.core.application.model;

import java.util.List;

/**
 * Provider-normalized identity. Provider roles are informational and never become JWT authorities directly.
 */
public record SsoUserProfile(
        String subject,
        String email,
        String name,
        String departmentCode,
        List<String> roles) {

    public SsoUserProfile {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
