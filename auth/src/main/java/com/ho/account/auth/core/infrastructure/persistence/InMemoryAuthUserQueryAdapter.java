package com.ho.account.auth.core.infrastructure.persistence;

import com.ho.account.auth.core.application.port.out.AuthUserQueryPort;
import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class InMemoryAuthUserQueryAdapter implements AuthUserQueryPort {

    private final Map<String, AuthUser> usersByUsername;

    public InMemoryAuthUserQueryAdapter(AuthModuleProperties properties) {
        this.usersByUsername = properties.getUsers().stream()
                .map(this::toDomain)
                .collect(Collectors.toUnmodifiableMap(AuthUser::getUsername, Function.identity(), (first, second) -> second));
    }

    @Override
    public Optional<AuthUser> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(username));
    }

    private AuthUser toDomain(AuthModuleProperties.User user) {
        return new AuthUser(
                user.getUsername(),
                user.getPassword(),
                user.isActive(),
                user.isLocked(),
                user.getRoles());
    }
}

