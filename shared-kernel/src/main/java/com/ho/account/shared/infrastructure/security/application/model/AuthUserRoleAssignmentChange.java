package com.ho.account.shared.infrastructure.security.application.model;

import java.time.Instant;
import java.util.List;

public record AuthUserRoleAssignmentChange(
        String username,
        List<String> roleCodes,
        String dataScope,
        Instant validFrom,
        Instant validTo,
        String approvedBy,
        String approvalTraceId) {
}
