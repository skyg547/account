package com.ho.account.budget.infrastructure.persistence;

import com.ho.account.budget.domain.BudgetExecution;
import com.ho.account.budget.domain.BudgetExecutionStatus;
import org.springframework.stereotype.Component;

/** 집행 원천키와 취소 증적을 손실 없이 도메인 snapshot으로 복원합니다. */
@Component
class BudgetExecutionPersistenceMapper {

    BudgetExecution toDomain(BudgetExecutionJpaEntity entity) {
        return BudgetExecution.restore(
                entity.getId(),
                entity.getPlanId(),
                entity.getSourceType(),
                entity.getSourceId(),
                entity.getSourceLineId(),
                entity.getExecutionDate(),
                entity.getAmount(),
                BudgetExecutionStatus.valueOf(entity.getStatus()),
                entity.getExecutedBy(),
                entity.getCancelledBy());
    }

    BudgetExecutionJpaEntity newEntity(BudgetExecution domain) {
        BudgetExecutionJpaEntity entity = new BudgetExecutionJpaEntity();
        copy(domain, entity);
        return entity;
    }

    void copy(BudgetExecution domain, BudgetExecutionJpaEntity entity) {
        entity.setPlanId(domain.budgetPlanId());
        entity.setSourceType(domain.sourceType());
        entity.setSourceId(domain.sourceId());
        entity.setSourceLineId(domain.sourceLineId());
        entity.setExecutionDate(domain.executionDate());
        entity.setAmount(domain.amount());
        entity.setStatus(domain.status().name());
        entity.setExecutedBy(domain.executedBy());
        entity.setCancelledBy(domain.cancelledBy());
    }
}
