package com.ho.account.shared.infrastructure.security.application.port.out;

import com.ho.account.shared.infrastructure.security.application.model.AuthUserRoleAssignmentChange;

public interface AuthUserRoleAssignmentApplyPort {

    void replaceUserRoles(AuthUserRoleAssignmentChange change);
}
