package com.ho.account.auth.core.infrastructure.persistence;

import com.ho.account.auth.core.application.port.out.PatLifecycleEventPort;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Saves audit events in the PAT service transaction, so the PAT and event commit together. */
@Component
@RequiredArgsConstructor
public class PatLifecycleEventPersistenceAdapter implements PatLifecycleEventPort {

    private final PatLifecycleEventJpaRepository repository;

    @Override
    public void recordCreated(
            String actorUsername, String tokenId, String ownerUsername, LocalDateTime occurredAt) {
        repository.save(new PatLifecycleEventJpaEntity(
                tokenId, "CREATED", actorUsername, ownerUsername,
                "OWNER_CREATED", "ABSENT", "ACTIVE", occurredAt));
    }

    @Override
    public void recordRevoked(
            String actorUsername, String tokenId, String ownerUsername,
            String reasonCode, String beforeStatus, String afterStatus, LocalDateTime occurredAt) {
        repository.save(new PatLifecycleEventJpaEntity(
                tokenId, "REVOKED", actorUsername, ownerUsername,
                reasonCode, beforeStatus, afterStatus, occurredAt));
    }
}
