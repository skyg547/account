package com.ho.account.auth.core.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 인증 사용자(AuthUser) 도메인 모델 - 시스템에 접근하려는 사용자의 핵심 인증 정보를 담고 있습니다.
 *
 * <p>🐣 [초보자를 위한 설명]
 * 이 클래스는 시스템에 로그인하려는 '사람' 또는 '시스템'을 나타냅니다.
 * 은행에 비유하자면, 은행 창구에 앉아있는 직원(행원)의 신분증과 같습니다.
 * 이 신분증에는 직원의 이름(username), 저장 비밀번호(storedPassword), 소속 부서(departmentCode),
 * 그리고 어떤 업무를 할 수 있는지(roleAssignments)가 적혀 있습니다.
 * 유효한 역할은 호출자가 전달한 한 시점을 기준으로 계산하여 로그인 응답과 JWT가 같은 결과를 사용합니다.</p>
 */
public class AuthUser {

    private final String username;
    private final String storedPassword;
    private final String departmentCode;
    private final boolean active;
    private final boolean locked;
    private final List<RoleAssignment> roleAssignments;
    private final long roleVersion;

    public AuthUser(String username, String storedPassword, boolean active, boolean locked, List<String> roles) {
        this(username, storedPassword, null, active, locked, roles);
    }

    public AuthUser(
            String username,
            String storedPassword,
            String departmentCode,
            boolean active,
            boolean locked,
            List<String> roles) {
        this(username, storedPassword, departmentCode, active, locked, toAssignments(roles), 1L);
    }

    public AuthUser(
            String username,
            String storedPassword,
            String departmentCode,
            boolean active,
            boolean locked,
            List<RoleAssignment> roleAssignments,
            long roleVersion) {
        this.username = requireText(username, "username is required.");
        this.storedPassword = requireText(storedPassword, "storedPassword is required.");
        this.departmentCode = normalize(departmentCode);
        this.active = active;
        this.locked = locked;
        this.roleAssignments = roleAssignments == null ? List.of() : List.copyOf(roleAssignments);
        if (roleVersion < 1) {
            throw new IllegalArgumentException("roleVersion must be 1 or greater.");
        }
        this.roleVersion = roleVersion;
    }

    public String getUsername() {
        return username;
    }

    public String getStoredPassword() {
        return storedPassword;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isLocked() {
        return locked;
    }

    public List<RoleAssignment> getRoleAssignments() {
        return roleAssignments;
    }

    public long getRoleVersion() {
        return roleVersion;
    }

    public List<RoleAssignment> effectiveRoleAssignmentsAt(Instant evaluatedAt) {
        Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        return roleAssignments.stream()
                .filter(assignment -> assignment.isEffectiveAt(evaluatedAt))
                .toList();
    }

    public List<String> effectiveRolesAt(Instant evaluatedAt) {
        return effectiveRoleAssignmentsAt(evaluatedAt).stream()
                .map(RoleAssignment::roleCode)
                .distinct()
                .toList();
    }

    public boolean hasUsername(String otherUsername) {
        return Objects.equals(this.username, otherUsername);
    }

    private static List<RoleAssignment> toAssignments(List<String> roles) {
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .filter(Objects::nonNull)
                .filter(role -> !role.isBlank())
                .map(RoleAssignment::approved)
                .toList();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
