package com.ho.account.auth.core.infrastructure.persistence;

import com.ho.account.auth.core.application.port.out.AuthUserQueryPort;
import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class InMemoryAuthUserQueryAdapter implements AuthUserQueryPort {

    private final Map<String, AuthUser> usersByUsername;

    public InMemoryAuthUserQueryAdapter(AuthModuleProperties properties) {
        // @todo [검수-헥사고날] 설정 기반 인메모리 사용자 저장소는 로컬 부트스트랩 용도로 한정하고,
        //       운영 경로는 AuthUserPersistencePort/JPA 어댑터와 권한 변경 감사 로그를 통해 관리해야 한다.
        this.usersByUsername = new ConcurrentHashMap<>(
                properties.getUsers().stream()
                        .map(this::toDomain)
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

    public void updateRole(String username, List<String> newRoles) {
        AuthUser existing = usersByUsername.get(username);
        if (existing != null) {
            AuthUser updated = new AuthUser(
                    existing.getUsername(),
                    existing.getStoredPassword(),
                    existing.getDepartmentCode(),
                    existing.isActive(),
                    existing.isLocked(),
                    newRoles
            );
            usersByUsername.put(username, updated);
        }
    }

    private AuthUser toDomain(AuthModuleProperties.User user) {
        return new AuthUser(
                user.getUsername(),
                user.getPassword(),
                user.getDepartmentCode(),
                user.isActive(),
                user.isLocked(),
                user.getRoles());
    }
}
