package com.ho.account.auth.core.infrastructure.persistence;

import com.ho.account.auth.core.application.port.out.AuthUserRoleAssignmentPersistencePort;
import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.auth.core.domain.model.RoleAssignment;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "auth.persistence", name = "mode", havingValue = "memory")
public class InMemoryAuthUserRoleAssignmentAdapter implements AuthUserRoleAssignmentPersistencePort {

    private final InMemoryAuthUserQueryAdapter userQueryAdapter;

    @Override
    public AuthUser replaceRoleAssignments(String username, List<RoleAssignment> roleAssignments, String approvedBy) {
        userQueryAdapter.updateRole(username, roleAssignments.stream()
                .map(RoleAssignment::roleCode)
                .toList());
        return userQueryAdapter.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Auth user not found: " + username));
    }
}
