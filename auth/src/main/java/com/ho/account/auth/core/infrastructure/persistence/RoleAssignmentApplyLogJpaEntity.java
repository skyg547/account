package com.ho.account.auth.core.infrastructure.persistence;

import com.ho.account.auth.core.application.port.out.AuthUserRoleAssignmentPersistencePort;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Governance 역할 승인 호출의 수신 이력입니다.
 *
 * <p>approvalTraceId를 기본키로 사용해 외부 호출 성공 후 응답 유실/트랜잭션 재시도에서도
 * 역할 교체와 roleVersion 증가가 한 번만 일어나게 합니다.</p>
 */
// 이 멱등 이력은 단순 삭제하면 오래된 승인 재시도가 다시 반영될 수 있다.
// 내부회계 감사 보존기간과 최대 재시도 기간을 함께 반영한 archive/retention Job이 필요하다.
@Entity
@Table(name = "auth_role_assignment_apply_log")
class RoleAssignmentApplyLogJpaEntity {

    @Id
    @Column(name = "approval_trace_id", nullable = false, length = 160)
    private String approvalTraceId;

    @Column(name = "username", nullable = false, length = 80)
    private String username;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    @Column(name = "applied_role_version", nullable = false)
    private long appliedRoleVersion;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    protected RoleAssignmentApplyLogJpaEntity() {
    }

    RoleAssignmentApplyLogJpaEntity(
            String approvalTraceId,
            String username,
            String requestFingerprint,
            long appliedRoleVersion,
            Instant appliedAt) {
        this.approvalTraceId = approvalTraceId;
        this.username = username;
        this.requestFingerprint = requestFingerprint;
        this.appliedRoleVersion = appliedRoleVersion;
        this.appliedAt = appliedAt;
    }

    void validateSameRequest(AuthUserRoleAssignmentPersistencePort.RoleAssignmentReplacement replacement) {
        if (!username.equals(replacement.username())
                || !requestFingerprint.equals(replacement.requestFingerprint())) {
            throw new IllegalArgumentException(
                    "approvalTraceId was already used for a different role assignment request: "
                            + replacement.approvalTraceId());
        }
    }
}
