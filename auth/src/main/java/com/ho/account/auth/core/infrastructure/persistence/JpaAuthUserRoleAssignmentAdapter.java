package com.ho.account.auth.core.infrastructure.persistence;

import com.ho.account.auth.core.application.port.out.AuthUserRoleAssignmentPersistencePort;
import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.auth.core.domain.model.RoleAssignment;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
@ConditionalOnProperty(prefix = "auth.persistence", name = "mode", havingValue = "jpa", matchIfMissing = true)
public class JpaAuthUserRoleAssignmentAdapter implements AuthUserRoleAssignmentPersistencePort {

    private final AuthUserJpaRepository repository;

    @Override
    public AuthUser replaceRoleAssignments(String username, List<RoleAssignment> roleAssignments, String approvedBy) {
        AuthUserJpaEntity user = repository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Auth user not found: " + username));

        user.replaceRoleAssignments(roleAssignments.stream()
                .map(assignment -> RoleAssignmentJpaEntity.approvedBy(assignment, approvedBy))
                .toList());
        return repository.saveAndFlush(user).toDomain();
    }
}
