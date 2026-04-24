package com.ho.account.auth.api.dto;

import java.util.List;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        String username,
        List<String> roles) {
}

