package com.ho.account.auth.core.application.port.in;

import java.time.Instant;
import java.util.List;

public interface AuthUserRoleAssignmentUseCase {

    RoleAssignmentResult replaceRoleAssignments(ReplaceRoleAssignmentsCommand command);

    record ReplaceRoleAssignmentsCommand(
            String username,
            List<String> roleCodes,
            String dataScope,
            Instant validFrom,
            Instant validTo,
            String approvedBy,
            String approvalTraceId) {
    }

    record RoleAssignmentResult(
            String username,
            long roleVersion,
            List<String> roles) {
    }
}
