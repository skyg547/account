package com.ho.account.closing.application.service;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "account.closing.final-close-evidence")
public class FinalCloseEvidenceProperties {

    private static final int MAX_TRUSTED_SUBMITTERS = 64;
    private static final int MAX_ACTOR_LENGTH = 100;

    private Duration maxAge = Duration.ofHours(24);
    private Set<String> trustedSubmitters = Set.of();

    public Duration getMaxAge() {
        if (maxAge == null || maxAge.isZero() || maxAge.isNegative()) {
            throw new IllegalStateException("account.closing.final-close-evidence.max-age must be positive");
        }
        return maxAge;
    }

    public void setMaxAge(Duration maxAge) {
        this.maxAge = maxAge;
    }

    public Set<String> getTrustedSubmitters() {
        return trustedSubmitters;
    }

    public void setTrustedSubmitters(Set<String> trustedSubmitters) {
        if (trustedSubmitters == null) {
            this.trustedSubmitters = Set.of();
            return;
        }
        if (trustedSubmitters.size() > MAX_TRUSTED_SUBMITTERS) {
            throw new IllegalArgumentException(
                    "account.closing.final-close-evidence.trusted-submitters must contain at most "
                            + MAX_TRUSTED_SUBMITTERS + " actors");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String actor : trustedSubmitters) {
            normalized.add(normalizeConfiguredActor(actor));
        }
        this.trustedSubmitters = Collections.unmodifiableSet(normalized);
    }

    /** Exact, case-sensitive allowlist check after trimming transport whitespace. Empty config denies all. */
    public boolean isTrustedSubmitter(String actor) {
        if (actor == null || actor.isBlank()) {
            return false;
        }
        String normalized = actor.trim();
        return normalized.length() <= MAX_ACTOR_LENGTH && trustedSubmitters.contains(normalized);
    }

    private String normalizeConfiguredActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException(
                    "account.closing.final-close-evidence.trusted-submitters must not contain blank actors");
        }
        String normalized = actor.trim();
        if (normalized.length() > MAX_ACTOR_LENGTH) {
            throw new IllegalArgumentException(
                    "account.closing.final-close-evidence.trusted-submitters actor must not exceed "
                            + MAX_ACTOR_LENGTH + " characters");
        }
        return normalized;
    }
}
