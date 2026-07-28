package com.ho.account.auth.api.dto;

import java.time.LocalDateTime;

public record PersonalAccessTokenDto(
    String id,
    String username,
    String tokenName,
    String tokenPrefix,
    String status,
    LocalDateTime expiresAt,
    LocalDateTime lastUsedAt,
    LocalDateTime createdAt
) {}
