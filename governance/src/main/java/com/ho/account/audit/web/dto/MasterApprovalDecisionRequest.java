package com.ho.account.audit.web.dto;

import jakarta.validation.constraints.NotBlank;

public record MasterApprovalDecisionRequest(
        @NotBlank String approverUser,
        String remarks) {
}
