package com.ho.account.auth.core.application.service;

import com.ho.account.auth.core.application.port.in.AuthUserRoleAssignmentUseCase;
import com.ho.account.auth.core.application.port.out.AuthUserRoleAssignmentPersistencePort;
import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.auth.core.domain.model.RoleAssignment;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Governance에서 승인된 사용자 역할 목록을 Auth의 현재 역할로 교체합니다.
 *
 * <p>🐣 동일한 승인 요청이 네트워크 재시도로 여러 번 도착해도 approvalTraceId와
 * 요청 fingerprint가 같으면 저장 어댑터가 한 번만 반영합니다. 다른 내용이 같은
 * traceId를 재사용하면 조용히 덮어쓰지 않고 오류로 중단합니다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuthUserRoleAssignmentService implements AuthUserRoleAssignmentUseCase {

    private final AuthUserRoleAssignmentPersistencePort persistencePort;
    private final Clock clock;

    @Override
    public RoleAssignmentResult replaceRoleAssignments(ReplaceRoleAssignmentsCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Role assignment command is required.");
        }
        String username = requireText(command.username(), "username", 80);
        String approvedBy = requireText(command.approvedBy(), "approvedBy", 80);
        String approvalTraceId = requireText(command.approvalTraceId(), "approvalTraceId", 160);
        List<String> roleCodes = normalizeRoleCodes(command.roleCodes());
        if (roleCodes.isEmpty()) {
            throw new IllegalArgumentException("At least one roleCode is required.");
        }

        List<RoleAssignment> assignments = roleCodes.stream()
                .map(roleCode -> new RoleAssignment(
                        roleCode,
                        command.dataScope(),
                        command.validFrom(),
                        command.validTo(),
                        true))
                .toList();
        String fingerprint = fingerprint(username, assignments, approvedBy);

        AuthUser updated = persistencePort.replaceRoleAssignments(
                new AuthUserRoleAssignmentPersistencePort.RoleAssignmentReplacement(
                        username,
                        assignments,
                        approvedBy,
                        approvalTraceId,
                        fingerprint));
        return new RoleAssignmentResult(
                updated.getUsername(),
                updated.getRoleVersion(),
                updated.effectiveRolesAt(clock.instant()));
    }

    private List<String> normalizeRoleCodes(List<String> roleCodes) {
        if (roleCodes == null) {
            return List.of();
        }
        return roleCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(roleCode -> !roleCode.isBlank())
                .map(roleCode -> roleCode.toUpperCase(Locale.ROOT))
                .map(roleCode -> roleCode.startsWith("ROLE_") ? roleCode : "ROLE_" + roleCode)
                .distinct()
                .sorted()
                .toList();
    }

    private String fingerprint(String username, List<RoleAssignment> assignments, String approvedBy) {
        StringBuilder canonical = new StringBuilder();
        appendField(canonical, username);
        appendField(canonical, approvedBy);
        assignments.forEach(assignment -> {
            appendField(canonical, assignment.roleCode());
            appendField(canonical, assignment.dataScope());
            appendField(canonical, assignment.validFrom() == null ? null : assignment.validFrom().toString());
            appendField(canonical, assignment.validTo() == null ? null : assignment.validTo().toString());
            appendField(canonical, Boolean.toString(assignment.approved()));
        });
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required for role assignment idempotency.", exception);
        }
    }

    private void appendField(StringBuilder target, String value) {
        String normalized = value == null ? "<null>" : value;
        target.append(normalized.length()).append(':').append(normalized).append('|');
    }

    private String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " characters.");
        }
        return normalized;
    }
}
