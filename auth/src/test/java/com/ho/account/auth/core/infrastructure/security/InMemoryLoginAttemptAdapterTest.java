package com.ho.account.auth.core.infrastructure.security;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryLoginAttemptAdapterTest {

    @Test
    void locksAfterConfiguredConsecutiveFailuresAndClearsOnSuccess() {
        InMemoryLoginAttemptAdapter adapter = new InMemoryLoginAttemptAdapter(
                3, 15, Clock.fixed(Instant.parse("2026-06-09T00:00:00Z"), ZoneOffset.UTC));

        adapter.recordFailure("admin", "INVALID_PASSWORD");
        adapter.recordFailure("admin", "INVALID_PASSWORD");
        assertThat(adapter.isLocked("admin")).isFalse();

        adapter.recordFailure("admin", "INVALID_PASSWORD");
        assertThat(adapter.isLocked("admin")).isTrue();

        adapter.recordSuccess("admin");
        assertThat(adapter.isLocked("admin")).isFalse();
    }
}
