package com.ho.account.auth.core.infrastructure.persistence;

import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로컬 개발 및 테스트 환경('local' 프로파일)에서만 동작하는 데모 사용자 시드 러너입니다.
 *
 * <p>운영(prod)이나 통합 개발(dev) 환경에 실수로 auth.users 속성이 주입되더라도
 * 데모 계정이 자동 생성되는 보안 사고를 원천 방지하기 위해 @Profile("local")로 엄격히 격리합니다.</p>
 */
@Component
@Profile("local")
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