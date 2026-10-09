package com.ho.account.auth.core.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "auth_pat_lifecycle_events")
class PatLifecycleEventJpaEntity {

    @Id
    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "token_id", nullable = false, length = 36)
    private String tokenId;

    @Column(name = "action", nullable = false, length = 16)
    private String action;

    @Column(name = "actor_username", nullable = false, length = 80)
    private String actorUsername;

    @Column(name = "owner_username", nullable = false, length = 80)
    private String ownerUsername;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    protected PatLifecycleEventJpaEntity() {
    }

    PatLifecycleEventJpaEntity(
            String tokenId,
            String action,
            String actorUsername,
            String ownerUsername,
            LocalDateTime occurredAt) {
        this.eventId = UUID.randomUUID().toString();
        this.tokenId = Objects.requireNonNull(tokenId, "tokenId is required");
        this.action = Objects.requireNonNull(action, "action is required");
        this.actorUsername = Objects.requireNonNull(actorUsername, "actorUsername is required");
        this.ownerUsername = Objects.requireNonNull(ownerUsername, "ownerUsername is required");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
    }
}
