package com.ho.account.auth.core.infrastructure.persistence;

import com.ho.account.auth.core.application.port.out.AuthUserRepository;
import com.ho.account.auth.core.domain.model.AuthUser;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
@ConditionalOnProperty(prefix = "auth.persistence", name = "mode", havingValue = "jpa", matchIfMissing = true)
public class JpaAuthUserQueryAdapter implements AuthUserRepository {

    private final AuthUserJpaRepository repository;

    @Override
    public Optional<AuthUser> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return repository.findByUsername(username.trim())
                .map(AuthUserJpaEntity::toDomain);
    }

    @Override
    public List<AuthUser> findAllUsers() {
        return repository.findAllByOrderByUsernameAsc().stream()
                .map(AuthUserJpaEntity::toDomain)
                .toList();
    }
}
