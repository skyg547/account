package com.ho.account.auth.core.infrastructure.persistence;

import com.ho.account.auth.core.application.port.out.AuthUserRoleAssignmentPersistencePort;
import com.ho.account.auth.core.domain.model.AuthUser;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "auth.persistence", name = "mode", havingValue = "memory")
public class InMemoryAuthUserRoleAssignmentAdapter implements AuthUserRoleAssignmentPersistencePort {

    private final InMemoryAuthUserQueryAdapter userQueryAdapter;
    private final Map<String, AppliedRequest> appliedRequests = new HashMap<>();

    @Override
    public synchronized AuthUser replaceRoleAssignments(RoleAssignmentReplacement replacement) {
        AuthUser current = userQueryAdapter.findByUsername(replacement.username())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Auth user not found: " + replacement.username()));
        AppliedRequest applied = appliedRequests.get(replacement.approvalTraceId());
        if (applied != null) {
            applied.validateSameRequest(replacement);
            return current;
        }

        AuthUser updated = userQueryAdapter.replaceRoleAssignments(
                replacement.username(),
                replacement.roleAssignments());
        appliedRequests.put(
                replacement.approvalTraceId(),
                new AppliedRequest(replacement.username(), replacement.requestFingerprint()));
        return updated;
    }

    private record AppliedRequest(String username, String requestFingerprint) {
        private void validateSameRequest(RoleAssignmentReplacement replacement) {
            if (!username.equals(replacement.username())
                    || !requestFingerprint.equals(replacement.requestFingerprint())) {
                throw new IllegalArgumentException(
                        "approvalTraceId was already used for a different role assignment request: "
                                + replacement.approvalTraceId());
            }
        }
    }
}
