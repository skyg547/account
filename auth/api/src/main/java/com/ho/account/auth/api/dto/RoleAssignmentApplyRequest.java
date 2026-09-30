package com.ho.account.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public record RoleAssignmentApplyRequest(
        @NotEmpty List<@NotBlank String> roleCodes,
        @NotBlank String dataScope,
        Instant validFrom,
        Instant validTo,
        @NotBlank String approvedBy,
        @NotBlank @Size(max = 160) String approvalTraceId) {
}
