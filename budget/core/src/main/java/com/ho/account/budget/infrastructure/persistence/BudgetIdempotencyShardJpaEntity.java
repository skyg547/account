package com.ho.account.budget.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** 업무 결과 행이 생기기 전에 동일 멱등성 키를 직렬화하는 사전 생성 lock row입니다. */
@Entity
@Table(name = "budget_idempotency_shards")
public class BudgetIdempotencyShardJpaEntity {

    @Id
    @Column(name = "shard_id", nullable = false, updatable = false)
    private Short shardId;

    @Version
    @Column(nullable = false)
    private Long version;

    protected BudgetIdempotencyShardJpaEntity() {
    }

    Short getShardId() {
        return shardId;
    }

    Long getVersion() {
        return version;
    }
}
