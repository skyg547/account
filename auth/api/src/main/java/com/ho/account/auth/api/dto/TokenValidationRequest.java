package com.ho.account.auth.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record TokenValidationRequest(
        @NotBlank String username,
        @Min(1) long roleVersion
) {
}
