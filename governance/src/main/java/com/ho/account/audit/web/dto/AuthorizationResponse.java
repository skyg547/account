package com.ho.account.audit.web.dto;

import com.ho.account.audit.domain.AccessType;
import com.ho.account.audit.domain.Authorization;
import java.time.LocalDateTime;

public record AuthorizationResponse(
        Long id,
        String roleCode,
        String functionCode,
        AccessType accessType,
        String dataScope,
        LocalDateTime createDate,
        LocalDateTime updateDate,
        String auditUser) {

    public static AuthorizationResponse from(Authorization authorization) {
        String roleCode = authorization.getRole() != null ? authorization.getRole().getRoleCode() : null;
        return new AuthorizationResponse(
                authorization.getId(),
                roleCode,
                authorization.getFunctionCode(),
                authorization.getAccessType(),
                authorization.getDataScope(),
                authorization.getCreateDate(),
                authorization.getUpdateDate(),
                authorization.getAuditUser());
    }
}
