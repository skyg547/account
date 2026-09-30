package com.ho.account.auth.api.web;

import com.ho.account.auth.api.dto.AdminUserDto;
import com.ho.account.auth.core.application.exception.UserAccessDeniedException;
import com.ho.account.auth.core.application.port.in.AdminUserQueryUseCase;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserQueryUseCase adminUserQueryUseCase;

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> findAllUsers(HttpServletRequest request) {
        String username = requireSystemAdmin(request);
        List<AdminUserDto> response = adminUserQueryUseCase.findAllUsers(username).stream()
                .map(AdminUserDto::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    private String requireSystemAdmin(HttpServletRequest request) {
        Object userValue = request.getAttribute(AdminBearerAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE);
        String username = userValue instanceof String s && !s.isBlank()
                ? s
                : request.getHeader("X-Auth-User");

        Object rolesValue = request.getAttribute(AdminBearerAuthenticationFilter.AUTHENTICATED_ROLES_ATTRIBUTE);
        boolean authorized = rolesValue instanceof List<?> roles && roles.stream()
                .anyMatch(role -> "ROLE_SYSTEM_ADMIN".equals(role) || "SYSTEM_ADMIN".equals(role));
        if (!authorized && rolesValue == null) {
            String headerRoles = request.getHeader("X-Auth-Roles");
            authorized = headerRoles != null && Arrays.stream(headerRoles.split(","))
                    .map(String::trim)
                    .filter(role -> !role.isEmpty())
                    .map(role -> role.toUpperCase(Locale.ROOT))
                    .anyMatch(role -> "ROLE_SYSTEM_ADMIN".equals(role) || "SYSTEM_ADMIN".equals(role));
        }

        if (username == null || username.isBlank() || !authorized) {
            throw new UserAccessDeniedException("System administrator role is required.");
        }
        return username;
    }
}
