package com.ho.account.auth.core.domain.model;

/** Length policy shared by authenticated user identities and PAT owners. */
public final class AuthIdentityPolicy {

    public static final int MAX_USERNAME_LENGTH = 80;

    private AuthIdentityPolicy() {
    }
}
