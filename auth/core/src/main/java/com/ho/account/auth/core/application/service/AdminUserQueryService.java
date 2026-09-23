package com.ho.account.auth.core.application.service;

import com.ho.account.auth.core.application.model.AdminUserView;
import com.ho.account.auth.core.application.port.in.AdminUserQueryUseCase;
import com.ho.account.auth.core.application.port.out.AuthUserQueryPort;
import com.ho.account.auth.core.domain.model.AuthUser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserQueryService implements AdminUserQueryUseCase {

    private static final String DEFAULT_ROLE = "USER";

    private final AuthUserQueryPort authUserQueryPort;
    private final Clock clock;

    @Override
    public List<AdminUserView> findAllUsers() {
        Instant evaluatedAt = clock.instant();
        return authUserQueryPort.findAllUsers().stream()
                .sorted(Comparator.comparing(AuthUser::getUsername))
                .map(user -> toView(user, evaluatedAt))
                .toList();
    }

    private AdminUserView toView(AuthUser user, Instant evaluatedAt) {
        String username = user.getUsername();
        String role = user.effectiveRolesAt(evaluatedAt).stream()
                .findFirst()
                .map(this::toFrontendRole)
                .orElse(DEFAULT_ROLE);
        String status = user.isActive() && !user.isLocked() ? "ACTIVE" : "INACTIVE";
        String department = user.getDepartmentCode() == null ? "" : user.getDepartmentCode();

        return new AdminUserView(
                presentationId(username),
                username,
                username,
                role,
                status,
                "",
                department);
    }

    private String toFrontendRole(String roleCode) {
        return switch (roleCode.trim().toUpperCase(Locale.ROOT)) {
            case "ROLE_SYSTEM_ADMIN", "SYSTEM_ADMIN", "ROLE_ADMIN" -> "SYSTEM_ADMIN";
            case "ROLE_ACCOUNTING_ADMIN", "ACCOUNTING_ADMIN" -> "ACCOUNTING_ADMIN";
            case "ROLE_RISK_MANAGER", "RISK_MANAGER" -> "RISK_MANAGER";
            case "ROLE_RISK_ANALYST", "RISK_ANALYST" -> "RISK_ANALYST";
            case "ROLE_MASTER_MANAGER", "MASTER_MANAGER" -> "MASTER_MANAGER";
            case "ROLE_AUDITOR", "AUDITOR" -> "AUDITOR";
            case "ROLE_USER", "USER" -> "USER";
            default -> DEFAULT_ROLE;
        };
    }

    /**
     * Produces a stable 48-bit display key that is exactly representable by JavaScript Number.
     * It is not an identity guarantee: truncated hashes can collide and must never authorize or mutate users.
     */
    private long presentationId(String username) {
        byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-256")
                    .digest(username.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required for the admin user presentation ID.", exception);
        }

        long id = 0L;
        for (int index = 0; index < 6; index++) {
            id = (id << 8) | (digest[index] & 0xffL);
        }
        return id + 1L;
    }
}
