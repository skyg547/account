package com.ho.account.budget.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
interface SpringDataBudgetTransferRepository
        extends JpaRepository<BudgetTransferJpaEntity, Long> {

    Optional<BudgetTransferJpaEntity> findByRequestKey(String requestKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select transfer from BudgetTransferJpaEntity transfer where transfer.id = :id")
    Optional<BudgetTransferJpaEntity> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select transfer
            from BudgetTransferJpaEntity transfer
            where transfer.requestKey = :requestKey
            """)
    Optional<BudgetTransferJpaEntity> findByRequestKeyForUpdate(
            @Param("requestKey") String requestKey);
}
