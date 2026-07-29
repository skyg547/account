package com.ho.account.auth.core.infrastructure.security;

import com.ho.account.auth.core.application.port.out.LoginAttemptPort;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import java.time.Clock;
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
    @Transactional
    public boolean isLocked(String username) {
        String key = normalize(username);
        return repository.findById(key)
                .map(state -> {
                    boolean locked = state.isLocked(clock);
                    if (!locked && state.getLockedUntil() != null) {
                        repository.deleteById(key);
                    }
                    return locked;
                })
                .orElse(false);
    }

    @Override
    @Transactional
    public void recordFailure(String username, String reason) {
        String key = normalize(username);
        // 여러 노드가 같은 사용자의 "첫 실패"를 동시에 기록하면 find 후 insert가 경합할 수 있으므로
        // PostgreSQL/H2 양쪽에서 검증된 원자적 upsert 또는 사용자별 DB lock 경계로 전환해야 합니다.
        LoginAttemptJpaEntity state = repository.findById(key)
                .orElseGet(() -> new LoginAttemptJpaEntity(key));
        state.recordFailure(reason, maxFailures, lockDurationMinutes, clock);
        repository.save(state);
        log.warn("AUTH_LOGIN_FAILURE username={} reason={} failures={} lockedUntil={}",
                key, reason, state.getFailureCount(), state.getLockedUntil());
    }

    @Override
    @Transactional
    public void recordSuccess(String username) {
        String key = normalize(username);
        repository.deleteById(key);
        log.info("AUTH_LOGIN_SUCCESS username={}", key);
    }

    private String normalize(String username) {
        return username == null ? "<blank>" : username.trim().toLowerCase();
    }
}
