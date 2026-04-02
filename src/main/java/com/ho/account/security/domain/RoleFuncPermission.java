package com.ho.account.security.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "ROLE_FUNC_PERMISSIONS")
public class RoleFuncPermission {

    @EmbeddedId
    private RoleFuncPermissionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleCode")
    @JoinColumn(name = "ROLE_CODE")
    private SecurityRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("funcCode")
    @JoinColumn(name = "FUNC_CODE")
    private SystemFunction function;

    @Column(name = "CAN_READ", length = 1)
    private String canRead = "Y";

    @Column(name = "CAN_WRITE", length = 1)
    private String canWrite = "N";

    @Column(name = "CAN_DELETE", length = 1)
    private String canDelete = "N";

    @Column(name = "CAN_APPROVE", length = 1)
    private String canApprove = "N";

    @Column(name = "CREATE_DATE", nullable = false)
    private LocalDateTime createDate = LocalDateTime.now();

    @Column(name = "AUDIT_USER", nullable = false, length = 50)
    private String auditUser = "SYSTEM";

    // Getter 및 Setter
    public RoleFuncPermissionId getId() {
        return id;
    }

    public void setId(RoleFuncPermissionId id) {
        this.id = id;
    }

    public SecurityRole getRole() {
        return role;
    }

    public void setRole(SecurityRole role) {
        this.role = role;
    }

    public SystemFunction getFunction() {
        return function;
    }

    public void setFunction(SystemFunction function) {
        this.function = function;
    }

    public String getCanRead() {
        return canRead;
    }

    public void setCanRead(String canRead) {
        this.canRead = canRead;
    }

    public String getCanWrite() {
        return canWrite;
    }

    public void setCanWrite(String canWrite) {
        this.canWrite = canWrite;
    }

    public String getCanDelete() {
        return canDelete;
    }

    public void setCanDelete(String canDelete) {
        this.canDelete = canDelete;
    }

    public String getCanApprove() {
        return canApprove;
    }

    public void setCanApprove(String canApprove) {
        this.canApprove = canApprove;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }

    public String getAuditUser() {
        return auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }

    @Embeddable
    public static class RoleFuncPermissionId implements Serializable {
        private String roleCode;
        private String funcCode;

        public RoleFuncPermissionId() {
        }

        public RoleFuncPermissionId(String roleCode, String funcCode) {
            this.roleCode = roleCode;
            this.funcCode = funcCode;
        }

        // equals and hashCode
        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            RoleFuncPermissionId that = (RoleFuncPermissionId) o;
            return java.util.Objects.equals(roleCode, that.roleCode) &&
                    java.util.Objects.equals(funcCode, that.funcCode);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(roleCode, funcCode);
        }

        public String getRoleCode() {
            return roleCode;
        }

        public void setRoleCode(String roleCode) {
            this.roleCode = roleCode;
        }

        public String getFuncCode() {
            return funcCode;
        }

        public void setFuncCode(String funcCode) {
            this.funcCode = funcCode;
        }
    }
}
