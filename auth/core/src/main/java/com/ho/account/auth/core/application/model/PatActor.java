package com.ho.account.auth.core.application.model;

/** The canonical current account and authority established from a verified credential. */
public record PatActor(String username, boolean systemAdmin) {

    public PatActor {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("PAT actor username is required.");
        }
    }
}
