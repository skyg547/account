package com.ho.account.auth.api.web;

import com.ho.account.auth.api.dto.AdminUserDto;
import com.ho.account.auth.core.application.exception.UserAccessDeniedException;
import com.ho.account.auth.core.application.port.in.AdminUserQueryUseCase;
import java.util.List;
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
        requireSystemAdmin(request);
        List<AdminUserDto> response = adminUserQueryUseCase.findAllUsers().stream()
                .map(AdminUserDto::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    private void requireSystemAdmin(HttpServletRequest request) {
        Object value = request.getAttribute(AdminBearerAuthenticationFilter.AUTHENTICATED_ROLES_ATTRIBUTE);
        boolean authorized = value instanceof List<?> roles && roles.stream()
                .anyMatch(role -> "ROLE_SYSTEM_ADMIN".equals(role) || "SYSTEM_ADMIN".equals(role));
        if (!authorized) {
            throw new UserAccessDeniedException("System administrator role is required.");
        }
    }
}
