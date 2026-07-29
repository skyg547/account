package com.ho.account.shared.infrastructure.security.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRoleRequest(
        @NotBlank String roleCode,
        @NotBlank String roleName,
        String description) {
}
