package com.ho.account.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.List;

public record RoleAssignmentApplyRequest(
        @NotEmpty List<@NotBlank String> roleCodes,
        String dataScope,
        Instant validFrom,
        Instant validTo,
        @NotBlank String approvedBy,
        String approvalTraceId) {
}
