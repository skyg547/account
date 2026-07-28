package com.ho.account.auth.core.infrastructure.persistence;

import com.ho.account.auth.core.application.port.out.AuthUserQueryPort;
import com.ho.account.auth.core.domain.model.AuthUser;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
@ConditionalOnProperty(prefix = "auth.persistence", name = "mode", havingValue = "jpa", matchIfMissing = true)
public class JpaAuthUserQueryAdapter implements AuthUserQueryPort {

    private final AuthUserJpaRepository repository;

    @Override
    public Optional<AuthUser> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return repository.findByUsername(username.trim())
                .map(AuthUserJpaEntity::main);
    }
}
