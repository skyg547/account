package com.ho.account.shared.infrastructure.security.web.dto;

import jakarta.validation.constraints.NotBlank;

public record MasterApprovalDecisionRequest(
        @NotBlank String approverUser,
        String remarks) {
}
