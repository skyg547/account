package com.ho.account.auth.core.application.model;

import java.time.LocalDateTime;

public record PersonalAccessTokenCreateResponse(
    String id,
    String username,
    String tokenName,
    String rawToken,
    String tokenPrefix,
    String status,
    LocalDateTime expiresAt,
    LocalDateTime createdAt
) {}
