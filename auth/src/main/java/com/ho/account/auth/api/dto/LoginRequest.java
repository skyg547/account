package com.ho.account.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "username is required")
        String username,
        String password,
        String loginType,
        String otpCode) {
}

