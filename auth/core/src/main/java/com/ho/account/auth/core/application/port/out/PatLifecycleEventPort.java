package com.ho.account.auth.core.application.port.out;

import java.time.LocalDateTime;

/** Persists the actor and owner of a successful PAT lifecycle change. */
public interface PatLifecycleEventPort {

    void recordCreated(String actorUsername, String tokenId, String ownerUsername, LocalDateTime occurredAt);

    void recordRevoked(String actorUsername, String tokenId, String ownerUsername, LocalDateTime occurredAt);
}
