package com.ho.account.shared.infrastructure.security.web.dto;

import com.ho.account.shared.infrastructure.security.domain.AccessType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GrantAuthorizationRequest(
        @NotBlank String functionCode,
        @NotNull AccessType accessType,
        String dataScope) {
}
