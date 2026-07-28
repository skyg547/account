package com.ho.account.auth.core.infrastructure.persistence;

import com.ho.account.auth.core.application.port.out.AuthUserRoleAssignmentPersistencePort;
import com.ho.account.auth.core.domain.model.AuthUser;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
@ConditionalOnProperty(prefix = "auth.persistence", name = "mode", havingValue = "jpa", matchIfMissing = true)
public class JpaAuthUserRoleAssignmentAdapter implements AuthUserRoleAssignmentPersistencePort {

    private final AuthUserJpaRepository userRepository;
    private final RoleAssignmentApplyLogJpaRepository applyLogRepository;
    private final Clock clock;

    @Override
    public AuthUser replaceRoleAssignments(RoleAssignmentReplacement replacement) {
        // 같은 사용자의 역할 변경을 직렬화하여 서로 다른 승인 요청의 roleVersion 증가 순서를 고정합니다.
        AuthUserJpaEntity user = userRepository.findByUsernameForRoleAssignmentUpdate(replacement.username())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Auth user not found: " + replacement.username()));

        RoleAssignmentApplyLogJpaEntity applied = applyLogRepository
                .findById(replacement.approvalTraceId())
                .orElse(null);
        if (applied != null) {
            applied.validateSameRequest(replacement);
            return user.toDomain();
        }

        Instant appliedAt = clock.instant();
        user.replaceRoleAssignments(replacement.roleAssignments().stream()
                .map(assignment -> RoleAssignmentJpaEntity.approvedBy(
                        assignment, replacement.approvedBy(), appliedAt))
                .toList());
        AuthUser updated = userRepository.saveAndFlush(user).toDomain();
        applyLogRepository.saveAndFlush(new RoleAssignmentApplyLogJpaEntity(
                replacement.approvalTraceId(),
                replacement.username(),
                replacement.requestFingerprint(),
                updated.getRoleVersion(),
                appliedAt));
        return updated;
    }
}
