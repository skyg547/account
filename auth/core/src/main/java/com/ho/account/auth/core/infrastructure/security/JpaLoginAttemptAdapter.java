package com.ho.account.auth.core.infrastructure.security;

import com.ho.account.auth.core.application.port.out.LoginAttemptPort;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import java.time.Clock;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다중 인스턴스 운영에서 로그인 실패와 임시 잠금을 공유 DB에 저장하는 어댑터입니다.
 *
 * <p>🐣 서버가 여러 대이면 각 서버의 메모리가 서로 다르므로 실패 횟수가 흩어질 수 있습니다.
 * 이 구현은 같은 테이블을 보게 하여 어느 Auth 서버로 요청이 들어와도 같은 잠금 상태를 판단합니다.</p>
 */
@Component
@ConditionalOnProperty(prefix = "auth.login-security", name = "store", havingValue = "jpa")
public class JpaLoginAttemptAdapter implements LoginAttemptPort {

    private static final Logger log = LoggerFactory.getLogger(JpaLoginAttemptAdapter.class);

    private final LoginAttemptJpaRepository repository;
    private final int maxFailures;
    private final long lockDurationMinutes;
    private final Clock clock;

    @Autowired
    public JpaLoginAttemptAdapter(
            AuthModuleProperties properties,
            LoginAttemptJpaRepository repository,
            Clock clock) {
        this(repository,
                properties.getLoginSecurity().getMaxFailures(),
                properties.getLoginSecurity().getLockDurationMinutes(),
                clock);
    }

    JpaLoginAttemptAdapter(
            LoginAttemptJpaRepository repository,
            int maxFailures,
            long lockDurationMinutes,
            Clock clock) {
        if (maxFailures < 1) {
            throw new IllegalArgumentException("auth.login-security.max-failures must be at least 1");
        }
        if (lockDurationMinutes < 1) {
            throw new IllegalArgumentException("auth.login-security.lock-duration-minutes must be at least 1");
        }
        this.repository = repository;
        this.maxFailures = maxFailures;
        this.lockDurationMinutes = lockDurationMinutes;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLocked(String username) {
        return repository.findById(normalize(username))
                .map(state -> state.isLocked(clock))
                .orElse(false);
    }

    @Override
    @Transactional
    public void recordFailure(String username, String reason) {
        String key = normalize(username);
        LoginAttemptJpaEntity state = null;
        // A missing row cannot be SELECT-locked. The insert arbitrates first writers; the
        // locked read serializes increments, including a row deleted between insert and read.
        for (int attempt = 0; attempt < 3 && state == null; attempt++) {
            state = repository.findByUsernameForUpdate(key).orElse(null);
            if (state == null) {
                repository.insertPlaceholderIfAbsent(key, clock.instant());
                state = repository.findByUsernameForUpdate(key).orElse(null);
            }
        }
        if (state == null) {
            throw new IllegalStateException("Login attempt row changed during failure recording");
        }
        state.recordFailure(reason, maxFailures, lockDurationMinutes, clock);
        log.warn("AUTH_LOGIN_FAILURE username={} reason={} failures={} lockedUntil={}",
                key, reason, state.getFailureCount(), state.getLockedUntil());
    }

    @Override
    @Transactional
    public void recordSuccess(String username) {
        String key = normalize(username);
        // Recheck under the same row lock as failure recording so a refreshed lock survives.
        repository.findByUsernameForUpdate(key).ifPresent(state -> {
            if (!state.isLocked(clock)) {
                repository.delete(state);
            }
        });
        log.info("AUTH_LOGIN_SUCCESS username={}", key);
    }

    private String normalize(String username) {
        return username == null ? "<blank>" : username.trim().toLowerCase(Locale.ROOT);
    }
}
