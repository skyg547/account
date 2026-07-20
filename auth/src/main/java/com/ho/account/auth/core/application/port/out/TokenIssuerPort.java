package com.ho.account.auth.core.application.port.out;

import com.ho.account.auth.core.domain.model.RoleAssignment;
import java.time.Instant;
import java.util.List;

public interface TokenIssuerPort {

    IssuedToken issue(TokenSubject subject, Instant issuedAt);

    /**
     * 로그인 시점에 확정한 역할 스냅샷입니다.
     *
     * <p>JWT 어댑터가 다시 현재 시각을 조회하지 않게 하여 API 응답과 토큰 claim이
     * 서로 다른 역할 목록을 갖는 경계 시점 오류를 막습니다.</p>
     */
    record TokenSubject(
            String username,
            String departmentCode,
            List<RoleAssignment> effectiveRoleAssignments,
            long roleVersion) {

        public TokenSubject {
            if (username == null || username.isBlank()) {
                throw new IllegalArgumentException("Token subject username is required.");
            }
            effectiveRoleAssignments = effectiveRoleAssignments == null
                    ? List.of()
                    : List.copyOf(effectiveRoleAssignments);
            if (effectiveRoleAssignments.isEmpty()) {
                throw new IllegalArgumentException("At least one effective role assignment is required.");
            }
            if (roleVersion < 1) {
                throw new IllegalArgumentException("Token subject roleVersion must be 1 or greater.");
            }
        }
    }

    record IssuedToken(
            String token,
            long expiresInSeconds) {
    }
}
