package com.ho.account.budget.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
interface SpringDataBudgetFiscalYearControlRepository
        extends JpaRepository<BudgetFiscalYearControlJpaEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select control
            from BudgetFiscalYearControlJpaEntity control
            where control.fiscalYear = :fiscalYear
            """)
    Optional<BudgetFiscalYearControlJpaEntity> findByFiscalYearForUpdate(
            @Param("fiscalYear") String fiscalYear);
}
