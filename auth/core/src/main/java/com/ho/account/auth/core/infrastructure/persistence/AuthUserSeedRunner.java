package com.ho.account.auth.core.infrastructure.persistence;

import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "auth.persistence", name = "mode", havingValue = "jpa", matchIfMissing = true)
class AuthUserSeedRunner implements ApplicationRunner {

    private final AuthModuleProperties properties;
    private final AuthUserJpaRepository repository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        properties.getUsers().stream()
                .filter(user -> user.getUsername() != null && !user.getUsername().isBlank())
                .filter(user -> user.getPassword() != null && !user.getPassword().isBlank())
                .filter(user -> !repository.existsById(user.getUsername().trim()))
                .map(AuthUserJpaEntity::fromConfiguredUser)
                .forEach(repository::save);
    }
}
