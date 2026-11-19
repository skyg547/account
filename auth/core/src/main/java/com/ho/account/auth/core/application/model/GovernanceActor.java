package com.ho.account.auth.core.application.model;

import java.util.Set;

/** Identity and current GLOBAL roles resolved by the Auth credential boundary. */
public record GovernanceActor(String username, Set<String> roles) {
    public GovernanceActor {
        roles = Set.copyOf(roles);
    }

    public boolean systemAdmin() {
        return roles.contains("SYSTEM_ADMIN") || roles.contains("ROLE_SYSTEM_ADMIN");
    }
}
