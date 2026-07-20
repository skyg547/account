package com.ho.account.auth.api.dto;

import com.ho.account.auth.core.application.model.AuthenticationResult;
import java.util.List;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        String username,
        String departmentCode,
        List<String> roles,
        long roleVersion) {

    public static LoginResponse from(AuthenticationResult result) {
        return new LoginResponse(
                result.accessToken(),
                "Bearer",
                result.expiresInSeconds(),
                result.username(),
                result.departmentCode(),
                result.roles(),
                result.roleVersion());
    }
}
