package com.ho.account.internalaudit.core.infrastructure.persistence.repository;

import com.ho.account.internalaudit.core.infrastructure.persistence.entity.DesignEvaluationJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DesignEvaluationRepository extends JpaRepository<DesignEvaluationJpaEntity, String> {
    List<DesignEvaluationJpaEntity> findByControlId(String controlId);
}
