package com.ho.account.expenditure.repository;

import com.ho.account.expenditure.infrastructure.persistence.entity.PayableOffsetOperationJpaEntity;
import com.ho.account.expenditure.infrastructure.persistence.entity.PayableOffsetOperationId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayableOffsetOperationRepository extends JpaRepository<PayableOffsetOperationJpaEntity, PayableOffsetOperationId> {
}
