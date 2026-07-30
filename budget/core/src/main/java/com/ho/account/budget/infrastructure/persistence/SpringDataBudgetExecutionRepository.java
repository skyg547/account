package com.ho.account.budget.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
interface SpringDataBudgetExecutionRepository
        extends JpaRepository<BudgetExecutionJpaEntity, Long> {

    Optional<BudgetExecutionJpaEntity> findBySourceTypeAndSourceIdAndSourceLineId(
            String sourceType, String sourceId, String sourceLineId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select execution from BudgetExecutionJpaEntity execution where execution.id = :id")
    Optional<BudgetExecutionJpaEntity> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select execution
            from BudgetExecutionJpaEntity execution
            where execution.sourceType = :sourceType
              and execution.sourceId = :sourceId
              and execution.sourceLineId = :sourceLineId
            """)
    Optional<BudgetExecutionJpaEntity> findBySourceForUpdate(
            @Param("sourceType") String sourceType,
            @Param("sourceId") String sourceId,
            @Param("sourceLineId") String sourceLineId);
}
