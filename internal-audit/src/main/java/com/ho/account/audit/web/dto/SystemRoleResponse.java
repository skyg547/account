package com.ho.account.audit.web.dto;

import com.ho.account.audit.domain.SystemRole;
import java.time.LocalDateTime;
import java.util.List;

public record SystemRoleResponse(
        Long id,
        String roleCode,
        String roleName,
        String description,
        List<AuthorizationResponse> authorizations,
        LocalDateTime createDate,
        LocalDateTime updateDate,
        String auditUser) {

    public static SystemRoleResponse from(SystemRole role) {
        List<AuthorizationResponse> authorizations = role.getAuthorizations() == null
                ? List.of()
                : role.getAuthorizations().stream()
                        .map(AuthorizationResponse::from)
                        .toList();
        return new SystemRoleResponse(
                role.getId(),
                role.getRoleCode(),
                role.getRoleName(),
                role.getDescription(),
                authorizations,
                role.getCreateDate(),
                role.getUpdateDate(),
                role.getAuditUser());
    }
}
