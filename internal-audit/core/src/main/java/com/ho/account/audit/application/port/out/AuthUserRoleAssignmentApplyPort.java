package com.ho.account.audit.application.port.out;

import com.ho.account.audit.application.model.AuthUserRoleAssignmentChange;

public interface AuthUserRoleAssignmentApplyPort {

    void replaceUserRoles(AuthUserRoleAssignmentChange change);
}
