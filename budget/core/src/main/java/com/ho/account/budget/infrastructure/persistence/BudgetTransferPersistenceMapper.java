package com.ho.account.budget.infrastructure.persistence;

import com.ho.account.budget.domain.BudgetTransfer;
import com.ho.account.budget.domain.BudgetTransferStatus;
import org.springframework.stereotype.Component;

/** 전용 도메인에는 없는 DB 감사 시각을 엔티티에 보존하며 상태 값만 번역합니다. */
@Component
class BudgetTransferPersistenceMapper {

    BudgetTransfer toDomain(BudgetTransferJpaEntity entity) {
        return BudgetTransfer.restore(
                entity.getId(),
                entity.getRequestKey(),
                entity.getFromPlanId(),
                entity.getToPlanId(),
                entity.getAmount(),
                BudgetTransferStatus.valueOf(entity.getStatus()),
                entity.getRequestedBy(),
                entity.getApprovedBy());
    }

    BudgetTransferJpaEntity newEntity(BudgetTransfer domain) {
        BudgetTransferJpaEntity entity = new BudgetTransferJpaEntity();
        copy(domain, entity);
        return entity;
    }

    void copy(BudgetTransfer domain, BudgetTransferJpaEntity entity) {
        entity.setRequestKey(domain.requestKey());
        entity.setFromPlanId(domain.sourcePlanId());
        entity.setToPlanId(domain.targetPlanId());
        entity.setAmount(domain.amount());
        entity.setStatus(domain.status().name());
        entity.setRequestedBy(domain.requestedBy());
        entity.setApprovedBy(domain.approvedBy());
    }
}
