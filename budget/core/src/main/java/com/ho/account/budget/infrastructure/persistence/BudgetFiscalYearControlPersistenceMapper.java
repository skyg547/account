package com.ho.account.budget.infrastructure.persistence;

import com.ho.account.budget.domain.BudgetFiscalYearControl;
import com.ho.account.budget.domain.BudgetFiscalYearStatus;
import org.springframework.stereotype.Component;

/** 회계연도 도메인 상태와 사전 생성된 JPA 잠금 행 사이를 번역합니다. */
@Component
class BudgetFiscalYearControlPersistenceMapper {

    BudgetFiscalYearControl toDomain(BudgetFiscalYearControlJpaEntity entity) {
        return BudgetFiscalYearControl.restore(
                entity.getFiscalYear(),
                BudgetFiscalYearStatus.valueOf(entity.getStatus()),
                entity.getClosedBy());
    }

    void copy(
            BudgetFiscalYearControl domain,
            BudgetFiscalYearControlJpaEntity entity) {
        entity.setStatus(domain.status().name());
        entity.setClosedBy(domain.closedBy());
    }
}
