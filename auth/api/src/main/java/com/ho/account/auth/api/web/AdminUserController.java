package com.ho.account.auth.api.web;

import com.ho.account.auth.api.dto.AdminUserDto;
import com.ho.account.auth.core.application.exception.UserAccessDeniedException;
import com.ho.account.auth.core.application.port.in.AdminUserQueryUseCase;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUserController {

    static final String AUTH_ROLES_HEADER = "X-Auth-Roles";
    static final String AUTH_USER_HEADER = "X-Auth-User";

    private final AdminUserQueryUseCase adminUserQueryUseCase;

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> findAllUsers(
            @RequestHeader(value = AUTH_ROLES_HEADER, required = false) String authenticatedRoles,
            @RequestHeader(value = AUTH_USER_HEADER, required = false) String authenticatedUsername) {
        requireSystemAdmin(authenticatedRoles);
        if (authenticatedUsername == null || authenticatedUsername.isBlank()) {
            throw new UserAccessDeniedException("Authenticated user is required.");
        }
        // Core checks the current stored assignment; the gateway role header is only a first gate.
        List<AdminUserDto> response = adminUserQueryUseCase.findAllUsers(authenticatedUsername.trim()).stream()
                .map(AdminUserDto::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    private void requireSystemAdmin(String authenticatedRoles) {
        boolean authorized = authenticatedRoles != null && Arrays.stream(authenticatedRoles.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> role.toUpperCase(Locale.ROOT))
                .anyMatch(role -> role.equals("ROLE_SYSTEM_ADMIN") || role.equals("SYSTEM_ADMIN"));
        if (!authorized) {
            throw new UserAccessDeniedException("System administrator role is required.");
        }
    }
}
