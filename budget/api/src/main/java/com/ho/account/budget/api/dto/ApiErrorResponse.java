package com.ho.account.budget.api.dto;

/** Stable, entity-free error body returned by the HTTP adapter. */
public record ApiErrorResponse(int status, String code, String message) {
}
