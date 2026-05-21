package com.ho.account.auth.core.application.port.out;

import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.auth.core.domain.model.RoleAssignment;
import java.util.List;

public interface AuthUserRoleAssignmentPersistencePort {

    AuthUser replaceRoleAssignments(String username, List<RoleAssignment> roleAssignments, String approvedBy);
}
