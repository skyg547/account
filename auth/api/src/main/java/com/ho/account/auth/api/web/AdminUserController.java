package com.ho.account.auth.api.web;

import com.ho.account.auth.api.dto.AdminUserDto;
import com.ho.account.auth.core.application.model.AdminUserView;
import com.ho.account.auth.core.application.exception.UserAccessDeniedException;
import com.ho.account.auth.core.application.port.in.AdminUserQueryUseCase;
import com.ho.account.auth.core.application.model.ListPage;
import com.ho.account.auth.core.application.model.ListPageRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserQueryUseCase adminUserQueryUseCase;

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> findAllUsers(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String username = requireSystemAdmin(request);
        ListPageRequest pageRequest = new ListPageRequest(page, size);
        ListPage<AdminUserView> result = adminUserQueryUseCase.findUsers(username, pageRequest);
        return ResponseEntity.ok()
                .header("X-Page", String.valueOf(result.page()))
                .header("X-Page-Size", String.valueOf(result.size()))
                .header("X-Has-Next", String.valueOf(result.hasNext()))
                .body(result.content().stream().map(AdminUserDto::from).toList());
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
