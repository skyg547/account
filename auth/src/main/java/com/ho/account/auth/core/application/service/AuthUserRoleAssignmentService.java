package com.ho.account.auth.core.application.service;

import com.ho.account.auth.core.application.port.in.AuthUserRoleAssignmentUseCase;
import com.ho.account.auth.core.application.port.out.AuthUserRoleAssignmentPersistencePort;
import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.auth.core.domain.model.RoleAssignment;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthUserRoleAssignmentService implements AuthUserRoleAssignmentUseCase {

    private final AuthUserRoleAssignmentPersistencePort persistencePort;

    @Override
    public RoleAssignmentResult replaceRoleAssignments(ReplaceRoleAssignmentsCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Role assignment command is required.");
        }
        String username = requireText(command.username(), "username is required.");
        String approvedBy = requireText(command.approvedBy(), "approvedBy is required.");
        List<String> roleCodes = normalizeRoleCodes(command.roleCodes());
        if (roleCodes.isEmpty()) {
            throw new IllegalArgumentException("At least one roleCode is required.");
        }

        List<RoleAssignment> assignments = roleCodes.stream()
                .map(roleCode -> new RoleAssignment(
                        roleCode,
                        command.dataScope(),
                        command.validFrom(),
                        command.validTo(),
                        true))
                .toList();

        AuthUser updated = persistencePort.replaceRoleAssignments(username, assignments, approvedBy);
        return new RoleAssignmentResult(updated.getUsername(), updated.getRoleVersion(), updated.getRoles());
    }

    private List<String> normalizeRoleCodes(List<String> roleCodes) {
        if (roleCodes == null) {
            return List.of();
        }
        return roleCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(roleCode -> !roleCode.isBlank())
                .map(roleCode -> roleCode.toUpperCase(Locale.ROOT))
                .map(roleCode -> roleCode.startsWith("ROLE_") ? roleCode : "ROLE_" + roleCode)
                .distinct()
                .toList();
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
