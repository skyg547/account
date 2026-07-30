package com.ho.account.budget.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
interface SpringDataBudgetPlanRepository extends JpaRepository<BudgetPlanJpaEntity, Long> {

    Optional<BudgetPlanJpaEntity> findByPlanCode(String planCode);

    Optional<BudgetPlanJpaEntity> findByYearMonthAndDepartmentCodeAndAccountCode(
            String yearMonth, String departmentCode, String accountCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select plan from BudgetPlanJpaEntity plan where plan.id = :id")
    Optional<BudgetPlanJpaEntity> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select plan
            from BudgetPlanJpaEntity plan
            where plan.yearMonth = :yearMonth
              and plan.departmentCode = :departmentCode
              and plan.accountCode = :accountCode
            """)
    Optional<BudgetPlanJpaEntity> findByBusinessKeyForUpdate(
            @Param("yearMonth") String yearMonth,
            @Param("departmentCode") String departmentCode,
            @Param("accountCode") String accountCode);

    /**
     * 정렬은 표현 목적이 아니라 잠금 순서 계약입니다. 모든 연말 마감이 같은 id 순서로
     * 행을 잠그면 서로 다른 트랜잭션이 반대 순서로 기다리는 교착 가능성을 낮춥니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select plan
            from BudgetPlanJpaEntity plan
            where plan.yearMonth like concat(:fiscalYear, '%')
              and plan.status = 'APPROVED'
            order by plan.id
            """)
    List<BudgetPlanJpaEntity> findApprovedByFiscalYearForUpdateOrderById(
            @Param("fiscalYear") String fiscalYear);
}
