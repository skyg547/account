package com.ho.account.budget.infrastructure.persistence;

import com.ho.account.budget.domain.BudgetPlan;
import com.ho.account.budget.domain.BudgetPlanStatus;
import org.springframework.stereotype.Component;

/** 순수 도메인 snapshot과 변경 추적 가능한 JPA 행 사이의 명시적 번역기입니다. */
@Component
class BudgetPlanPersistenceMapper {

    BudgetPlan toDomain(BudgetPlanJpaEntity entity) {
        return BudgetPlan.restore(
                entity.getId(),
                entity.getPlanCode(),
                entity.getYearMonth(),
                entity.getDepartmentCode(),
                entity.getAccountCode(),
                entity.getAllocatedAmount(),
                entity.getTransferredInAmount(),
                entity.getTransferredOutAmount(),
                entity.getExecutedAmount(),
                BudgetPlanStatus.valueOf(entity.getStatus()),
                entity.getCreatedBy(),
                entity.getApprovedBy(),
                entity.getClosedBy());
    }

    BudgetPlanJpaEntity newEntity(BudgetPlan domain) {
        BudgetPlanJpaEntity entity = new BudgetPlanJpaEntity();
        copy(domain, entity);
        return entity;
    }

    void copy(BudgetPlan domain, BudgetPlanJpaEntity entity) {
        entity.setPlanCode(domain.planCode());
        entity.setYearMonth(domain.yearMonth());
        entity.setDepartmentCode(domain.departmentCode());
        entity.setAccountCode(domain.accountCode());
        entity.setAllocatedAmount(domain.allocatedAmount());
        entity.setTransferredInAmount(domain.transferInAmount());
        entity.setTransferredOutAmount(domain.transferOutAmount());
        entity.setExecutedAmount(domain.executedAmount());
        entity.setStatus(domain.status().name());
        entity.setCreatedBy(domain.createdBy());
        entity.setApprovedBy(domain.approvedBy());
        entity.setClosedBy(domain.closedBy());
        entity.setAuditUser(latestActor(domain));
    }

    private String latestActor(BudgetPlan domain) {
        if (domain.closedBy() != null) {
            return domain.closedBy();
        }
        if (domain.approvedBy() != null) {
            return domain.approvedBy();
        }
        return domain.createdBy();
    }
}
