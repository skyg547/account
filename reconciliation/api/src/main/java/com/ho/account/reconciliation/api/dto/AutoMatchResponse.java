package com.ho.account.reconciliation.api.dto;

import java.util.Objects;

/** Outcome of an inter-branch auto-match request. */
public record AutoMatchResponse(int matchedCount, Status status) {

    public AutoMatchResponse {
        Objects.requireNonNull(status, "status must not be null");
    }

    public enum Status {
        NOT_EXECUTED
    }
}
