package com.ho.account.auth.core.application.port.out;

import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.auth.core.domain.model.RoleAssignment;
import java.util.List;

public interface AuthUserRoleAssignmentPersistencePort {

    AuthUser replaceRoleAssignments(RoleAssignmentReplacement replacement);

    /**
     * 승인된 역할 교체와 재시도 멱등 정보를 함께 전달하는 출력 포트 명령입니다.
     */
    record RoleAssignmentReplacement(
            String username,
            List<RoleAssignment> roleAssignments,
            String approvedBy,
            String approvalTraceId,
            String requestFingerprint) {

        public RoleAssignmentReplacement {
            roleAssignments = roleAssignments == null ? List.of() : List.copyOf(roleAssignments);
        }
    }
}
