package com.ho.account.budget.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
interface SpringDataBudgetIdempotencyShardRepository
        extends JpaRepository<BudgetIdempotencyShardJpaEntity, Short> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select shard
            from BudgetIdempotencyShardJpaEntity shard
            where shard.shardId = :shardId
            """)
    Optional<BudgetIdempotencyShardJpaEntity> findByShardIdForUpdate(
            @Param("shardId") Short shardId);
}
