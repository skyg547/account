package com.ho.account.audit.web.dto;

import com.ho.account.audit.domain.MasterApproval;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record MasterApprovalRequest(
        @NotBlank String masterType,
        @NotBlank String masterKey,
        @NotNull MasterApproval.ChangeRequestType requestType,
        String payload,
        @NotBlank String requestUser,
        LocalDate effectiveDate,
        Integer requestedVersion) {
}
