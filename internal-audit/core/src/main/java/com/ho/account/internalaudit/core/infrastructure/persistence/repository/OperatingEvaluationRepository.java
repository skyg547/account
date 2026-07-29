package com.ho.account.internalaudit.core.infrastructure.persistence.repository;

import com.ho.account.internalaudit.core.infrastructure.persistence.entity.OperatingEvaluationJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperatingEvaluationRepository extends JpaRepository<OperatingEvaluationJpaEntity, String> {
    List<OperatingEvaluationJpaEntity> findByControlId(String controlId);
}
