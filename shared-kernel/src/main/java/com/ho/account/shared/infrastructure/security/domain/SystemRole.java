package com.ho.account.shared.infrastructure.security.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 시스템 역할(SystemRole) 도메인 엔티티 — 시스템 내의 사용자 직무와 역할을 정의합니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 회사 내의 '직급' 또는 '담당 업무명'과 같습니다.
 * 예를 들어 "회계 팀장", "일반 행원", "시스템 관리자" 같은 이름표(Role)를 만들어 둡니다.
 * 이 이름표 하나에 여러 가지 권한(Authorizations)을 묶어두고, 
 * 사용자에게 이름표만 달아주면 관련된 모든 권한이 한 번에 부여되는 구조(RBAC)입니다.
 */
@Entity
@Table(name = "SYSTEM_ROLE")
public class SystemRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ROLE_CODE", nullable = false, unique = true, length = 50)
    private String roleCode;

    @Column(name = "ROLE_NAME", nullable = false, length = 100)
    private String roleName;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Authorization> authorizations = new ArrayList<>();

    @Column(name = "CREATE_DATE", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Column(name = "UPDATE_DATE", nullable = false)
    private LocalDateTime updateDate;

    @Column(name = "AUDIT_USER", nullable = false, length = 50)
    private String auditUser;

    public static SystemRole create(String roleCode, String roleName, String description, String auditUser) {
        SystemRole role = new SystemRole();
        role.roleCode = requireText(roleCode, "Role code is required.");
        role.roleName = requireText(roleName, "Role name is required.");
        role.description = description;
        role.auditUser = normalizeAuditUser(auditUser);
        return role;
    }

    public Authorization grant(String functionCode, AccessType accessType, String dataScope, String auditUser) {
        Authorization authorization = Authorization.grant(this, functionCode, accessType, dataScope, auditUser);
        this.authorizations.add(authorization);
        return authorization;
    }

    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
        this.updateDate = LocalDateTime.now();
        if (this.auditUser == null)
            this.auditUser = "SYSTEM";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateDate = LocalDateTime.now();
    }

    // Getter 및 Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Authorization> getAuthorizations() {
        return authorizations;
    }

    public void setAuthorizations(List<Authorization> authorizations) {
        this.authorizations = authorizations;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }

    public String getAuditUser() {
        return auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String normalizeAuditUser(String auditUser) {
        return auditUser == null || auditUser.isBlank() ? "SYSTEM" : auditUser.trim();
    }
}
