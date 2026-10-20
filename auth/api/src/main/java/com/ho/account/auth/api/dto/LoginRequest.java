package com.ho.account.auth.api.dto;

import com.ho.account.auth.core.domain.model.AuthIdentityPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "username is required")
        @Size(max = AuthIdentityPolicy.MAX_USERNAME_LENGTH, message = "username must be at most 80 characters")
        String username,
        String password,
        String loginType,
        String otpCode,
        String ssoProvider) {

    public LoginRequest(String username, String password, String loginType, String otpCode) {
        this(username, password, loginType, otpCode, null);
    }
}
