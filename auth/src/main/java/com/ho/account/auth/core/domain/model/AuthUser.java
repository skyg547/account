package com.ho.account.auth.core.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * 인증 사용자(AuthUser) 도메인 모델 — 시스템에 접근하려는 사용자의 핵심 인증 정보를 담고 있습니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 시스템에 로그인하려는 '사람' 또는 '시스템'을 나타냅니다.
 * 은행에 비유하자면, 은행 창구에 앉아있는 직원(행원)의 신분증과 같습니다.
 * 이 신분증에는 직원의 이름(username), 비밀번호(storedPassword), 소속 부서(departmentCode), 
 * 그리고 어떤 업무를 할 수 있는지(roles)가 적혀 있습니다.
 * 시스템은 이 객체를 확인하여 사용자가 진짜 누구인지, 로그인이 가능한 상태인지(active, locked) 판단합니다.
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

    public AuthUser(String username, String storedPassword, String departmentCode, boolean active, boolean locked,
            List<String> roles) {
        this(username, storedPassword, departmentCode, active, locked, toAssignments(roles), 1L);
    }

    public AuthUser(String username, String storedPassword, String departmentCode, boolean active, boolean locked,
            List<RoleAssignment> roleAssignments, long roleVersion) {
        this.username = username;
        this.storedPassword = storedPassword;
        this.departmentCode = departmentCode;
        this.active = active;
        this.locked = locked;
        this.roleAssignments = roleAssignments == null ? List.of() : List.copyOf(roleAssignments);
        this.roleVersion = Math.max(1L, roleVersion);
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

    public List<String> getRoles() {
        return roleAssignments.stream()
                .filter(assignment -> assignment.isEffectiveAt(java.time.Instant.now()))
                .map(RoleAssignment::roleCode)
                .distinct()
                .toList();
    }

    public List<RoleAssignment> getRoleAssignments() {
        return roleAssignments;
    }

    public long getRoleVersion() {
        return roleVersion;
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
}
