package com.ho.account.auth.api.dto;

public record TokenValidationResponse(
        boolean valid,
        String reason
) {
}
