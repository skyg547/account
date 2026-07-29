package com.ho.account.shared.infrastructure.security.application.port.in;

import com.ho.account.shared.infrastructure.security.domain.AccessType;
import com.ho.account.shared.infrastructure.security.domain.Authorization;
import com.ho.account.shared.infrastructure.security.domain.MasterApproval;
import com.ho.account.shared.infrastructure.security.domain.SystemRole;
import java.util.List;

public interface AuthorizationUseCase {

    SystemRole createRole(CreateRoleCommand command);

    List<SystemRole> getAllRoles();

    SystemRole getRoleByCode(String roleCode);

    Authorization grantAuthorization(GrantAuthorizationCommand command);

    /**
     * 권한을 즉시 삭제하지 않고 승인 요청으로 등록합니다.
     */
    MasterApproval revokeAuthorization(Long authorizationId);

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
