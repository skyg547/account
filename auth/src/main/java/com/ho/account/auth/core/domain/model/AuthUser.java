package com.ho.account.auth.core.domain.model;

import java.util.List;
import java.util.Objects;

public class AuthUser {

    private final String username;
    private final String storedPassword;
    private final boolean active;
    private final boolean locked;
    private final List<String> roles;

    public AuthUser(String username, String storedPassword, boolean active, boolean locked, List<String> roles) {
        this.username = username;
        this.storedPassword = storedPassword;
        this.active = active;
        this.locked = locked;
        this.roles = roles == null ? List.of() : List.copyOf(roles);
    }

    public String getUsername() {
        return username;
    }

    public String getStoredPassword() {
        return storedPassword;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isLocked() {
        return locked;
    }

    public List<String> getRoles() {
        return roles;
    }

    public boolean hasUsername(String otherUsername) {
        return Objects.equals(this.username, otherUsername);
    }
}

