package com.ho.account.security.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "SYSTEM_FUNCTIONS")
public class SystemFunction {

    @Id
    @Column(name = "FUNC_CODE", length = 50)
    private String funcCode;

    @Column(name = "FUNC_NAME", nullable = false, length = 100)
    private String funcName;

    @Column(name = "PARENT_FUNC_CODE", length = 50)
    private String parentFuncCode;

    @Column(name = "FUNC_TYPE", length = 20)
    private String funcType;

    @Column(name = "CREATE_DATE", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Column(name = "AUDIT_USER", nullable = false, length = 50)
    private String auditUser = "SYSTEM";

    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
    }

    // Getter 및 Setter
    public String getFuncCode() {
        return funcCode;
    }

    public void setFuncCode(String funcCode) {
        this.funcCode = funcCode;
    }

    public String getFuncName() {
        return funcName;
    }

    public void setFuncName(String funcName) {
        this.funcName = funcName;
    }

    public String getParentFuncCode() {
        return parentFuncCode;
    }

    public void setParentFuncCode(String parentFuncCode) {
        this.parentFuncCode = parentFuncCode;
    }

    public String getFuncType() {
        return funcType;
    }

    public void setFuncType(String funcType) {
        this.funcType = funcType;
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
}
