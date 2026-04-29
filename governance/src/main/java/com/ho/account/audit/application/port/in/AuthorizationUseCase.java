package com.ho.account.audit.application.port.in;

import com.ho.account.audit.domain.AccessType;
import com.ho.account.audit.domain.Authorization;
import com.ho.account.audit.domain.SystemRole;
import java.util.List;

public interface AuthorizationUseCase {

    SystemRole createRole(CreateRoleCommand command);

    List<SystemRole> getAllRoles();

    SystemRole getRoleByCode(String roleCode);

    Authorization grantAuthorization(GrantAuthorizationCommand command);

    void revokeAuthorization(Long authorizationId);

    List<Authorization> getAuthorizationsByRole(String roleCode);

    boolean hasPermission(String roleCode, String functionCode, AccessType accessType);

    record CreateRoleCommand(
            String roleCode,
            String roleName,
            String description) {
    }

    record GrantAuthorizationCommand(
            String roleCode,
            String functionCode,
            AccessType accessType,
            String dataScope) {
    }
}

