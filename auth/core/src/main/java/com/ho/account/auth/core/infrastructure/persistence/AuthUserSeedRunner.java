package com.ho.account.auth.core.infrastructure.persistence;

import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import java.util.List;
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
        // 입력 하나가 잘못된 경우 기존 사용자 조회나 부분 저장이 시작되지 않도록 전체를 먼저 구체화한다.
        properties.validateConfiguredUsers();
        List<AuthUserJpaEntity> configuredUsers = properties.getUsers().stream()
                .map(AuthUserJpaEntity::fromConfiguredUser)
                .toList();

        for (AuthUserJpaEntity user : configuredUsers) {
            if (!repository.existsById(user.getUsername())) {
                repository.save(user);
            }
        }
    }
}
