package com.ho.account.auth.core.infrastructure.persistence;

import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.auth.core.domain.model.RoleAssignment;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "auth_users")
class AuthUserJpaEntity {

    @Id
    @Column(name = "username", nullable = false, length = 80)
    private String username;

    @Column(name = "stored_password", nullable = false, length = 255)
    private String storedPassword;

    @Column(name = "department_code", length = 40)
    private String departmentCode;

    @Column(name = "account_active", nullable = false)
    private boolean active;

    @Column(name = "account_locked", nullable = false)
    private boolean locked;

    @Column(name = "role_version", nullable = false)
    private long roleVersion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<RoleAssignmentJpaEntity> roleAssignments = new ArrayList<>();

    protected AuthUserJpaEntity() {
    }

    AuthUserJpaEntity(
            String username,
            String storedPassword,
            String departmentCode,
            boolean active,
            boolean locked,
            long roleVersion) {
        this.username = requireText(username, "username is required.");
        this.storedPassword = requireText(storedPassword, "storedPassword is required.");
        this.departmentCode = normalize(departmentCode);
        this.active = active;
        this.locked = locked;
        this.roleVersion = Math.max(1L, roleVersion);
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    static AuthUserJpaEntity fromConfiguredUser(AuthModuleProperties.User user) {
        AuthUserJpaEntity entity = new AuthUserJpaEntity(
                user.getUsername(),
                user.getPassword(),
                user.getDepartmentCode(),
                user.isActive(),
                user.isLocked(),
                1L);
        List<String> roles = user.getRoles() == null ? List.of() : user.getRoles();
        roles.stream()
                .filter(Objects::nonNull)
                .filter(role -> !role.isBlank())
                .map(RoleAssignment::approved)
                .map(RoleAssignmentJpaEntity::from)
                .forEach(entity::addRoleAssignment);
        return entity;
    }

    AuthUser toDomain() {
        return new AuthUser(
                username,
                storedPassword,
                departmentCode,
                active,
                locked,
                roleAssignments.stream()
                        .map(RoleAssignmentJpaEntity::toDomain)
                        .toList(),
                roleVersion);
    }

    void addRoleAssignment(RoleAssignmentJpaEntity roleAssignment) {
        roleAssignment.assignUser(this);
        this.roleAssignments.add(roleAssignment);
        this.updatedAt = LocalDateTime.now();
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
