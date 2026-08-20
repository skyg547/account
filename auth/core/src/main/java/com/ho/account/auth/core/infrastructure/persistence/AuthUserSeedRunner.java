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
 * [AuthUserSeedRunner] 로컬 환경 전용 데모 사용자 시드 러너.
 *
 * 🐣 [초보자를 위한 개념 설명 및 보안 원칙]
 * - 데모 사용자 계정 자동 생성은 로컬 개발/테스트(`@Profile("local")`) 환경에서만 허용됩니다.
 * - `dev`, `prod` 등 운영 환경에서는 application.yml 또는 환경변수에 실수로 사용자 목록이 설정되어 있더라도,
 *   Fail-Closed 보안 원칙에 따라 이 러너가 절대 빈으로 등록되거나 실행되지 않아야 합니다.
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
