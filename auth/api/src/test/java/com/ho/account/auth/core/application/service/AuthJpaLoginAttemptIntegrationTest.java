package com.ho.account.auth.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.auth.AuthApplication;
import com.ho.account.auth.core.application.exception.InvalidCredentialsException;
import com.ho.account.auth.core.application.port.in.AuthUseCase;
import com.ho.account.auth.core.application.port.out.LoginAttemptPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 로그인 오케스트레이터 바깥의 JPA 쓰기 트랜잭션이 실제로 커밋되는지 확인합니다.
 */
@SpringBootTest(
        classes = AuthApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.loadbalancer.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false",
                "management.tracing.enabled=false",
                "spring.data.redis.repositories.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:auth-login-attempt;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "auth.persistence.mode=jpa",
                "auth.login-security.store=jpa",
                "auth.login-security.max-failures=1"
        })
class AuthJpaLoginAttemptIntegrationTest {

    @Autowired
    private AuthUseCase authUseCase;

    @Autowired
    private LoginAttemptPort loginAttemptPort;

    @BeforeEach
    void clearAttempt() {
        loginAttemptPort.recordSuccess("admin");
    }

    @Test
    void failedLoginCommitsAttemptInAdapterOwnedTransaction() {
        assertThatThrownBy(() -> authUseCase.login(new AuthUseCase.LoginCommand("admin", "wrong-password", "NORMAL", null)))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(loginAttemptPort.isLocked("admin")).isTrue();
    }
}
