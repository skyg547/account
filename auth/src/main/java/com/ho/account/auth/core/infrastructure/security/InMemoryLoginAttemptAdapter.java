package com.ho.account.auth.core.infrastructure.security;

import com.ho.account.auth.core.application.port.out.LoginAttemptPort;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 단일 인스턴스 환경에서 로그인 실패와 임시 잠금을 관리하는 기본 어댑터입니다.
 *
 * <p>🐣 실패 횟수는 비밀번호와 분리해서 보관합니다. 성공하면 실패 횟수를 지우고,
 * 정해진 횟수만큼 연속 실패하면 잠금 시간이 끝날 때까지 로그인을 차단합니다.</p>
 */
@Component
public class InMemoryLoginAttemptAdapter implements LoginAttemptPort {

    private static final Logger log = LoggerFactory.getLogger(InMemoryLoginAttemptAdapter.class);

    // @todo 다중 인스턴스 운영에서는 Redis/DB 기반 LoginAttemptPort 어댑터로 교체해 모든 Auth 서버가 같은 잠금 상태를 공유해야 한다.
    private final ConcurrentMap<String, AttemptState> attempts = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final long lockDurationMinutes;
    private final Clock clock;

    public InMemoryLoginAttemptAdapter(AuthModuleProperties properties) {
        this(properties.getLoginSecurity().getMaxFailures(),
                properties.getLoginSecurity().getLockDurationMinutes(),
                Clock.systemUTC());
    }

    InMemoryLoginAttemptAdapter(int maxFailures, long lockDurationMinutes, Clock clock) {
        if (maxFailures < 1) {
            throw new IllegalArgumentException("auth.login-security.max-failures must be at least 1");
        }
        if (lockDurationMinutes < 1) {
            throw new IllegalArgumentException("auth.login-security.lock-duration-minutes must be at least 1");
        }
        this.maxFailures = maxFailures;
        this.lockDurationMinutes = lockDurationMinutes;
        this.clock = clock;
    }

    @Override
    public boolean isLocked(String username) {
        AttemptState state = attempts.get(normalize(username));
        if (state == null || state.lockedUntil() == null) {
            return false;
        }
        if (!clock.instant().isBefore(state.lockedUntil())) {
            attempts.remove(normalize(username), state);
            return false;
        }
        return true;
    }

    @Override
    public void recordFailure(String username, String reason) {
        String key = normalize(username);
        AttemptState state = attempts.compute(key, (ignored, current) -> {
            int failures = current == null ? 1 : current.failures() + 1;
            Instant lockedUntil = failures >= maxFailures
                    ? clock.instant().plus(lockDurationMinutes, ChronoUnit.MINUTES)
                    : null;
            return new AttemptState(failures, lockedUntil);
        });
        log.warn("AUTH_LOGIN_FAILURE username={} reason={} failures={} lockedUntil={}",
                key, reason, state.failures(), state.lockedUntil());
    }

    @Override
    public void recordSuccess(String username) {
        String key = normalize(username);
        attempts.remove(key);
        log.info("AUTH_LOGIN_SUCCESS username={}", key);
    }

    private String normalize(String username) {
        return username == null ? "<blank>" : username.trim().toLowerCase();
    }

    private record AttemptState(int failures, Instant lockedUntil) {
    }
}
