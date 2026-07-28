package com.ho.account.auth.core.infrastructure.persistence;

import com.ho.account.auth.core.application.port.out.AuthUserQueryPort;
import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.auth.core.domain.model.RoleAssignment;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "auth.persistence", name = "mode", havingValue = "memory")
public class InMemoryAuthUserQueryAdapter implements AuthUserQueryPort {

    private final Map<String, AuthUser> usersByUsername;

    public InMemoryAuthUserQueryAdapter(AuthModuleProperties properties) {
        this.usersByUsername = new ConcurrentHashMap<>(
                properties.getUsers().stream()
                        .map(this::main)
                        .collect(Collectors.toMap(AuthUser::getUsername, Function.identity(), (first, second) -> second))
        );
    }

    @Override
    public Optional<AuthUser> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(username));
    }

    public List<AuthUser> findAll() {
        return new ArrayList<>(usersByUsername.values());
    }

    synchronized AuthUser replaceRoleAssignments(String username, List<RoleAssignment> newAssignments) {
        AuthUser existing = usersByUsername.get(username);
        if (existing == null) {
            throw new IllegalArgumentException("Auth user not found: " + username);
        }
        AuthUser updated = new AuthUser(
                existing.getUsername(),
                existing.getStoredPassword(),
                existing.getDepartmentCode(),
                existing.isActive(),
                existing.isLocked(),
                newAssignments,
                existing.getRoleVersion() + 1L);
        usersByUsername.put(username, updated);
        return updated;
    }

    private AuthUser main(AuthModuleProperties.User user) {
        return new AuthUser(
                user.getUsername(),
                user.getPassword(),
                user.getDepartmentCode(),
                user.isActive(),
                user.isLocked(),
                user.getRoles());
    }
}
