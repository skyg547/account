package com.ho.account.internalaudit.core.infrastructure.persistence.repository;

import com.ho.account.internalaudit.core.infrastructure.persistence.entity.DeficiencyJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeficiencyRepository extends JpaRepository<DeficiencyJpaEntity, String> {
    List<DeficiencyJpaEntity> findByEvaluationId(String evaluationId);
}
