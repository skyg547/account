package com.ho.account.internalaudit.core.infrastructure.persistence.repository;

import com.ho.account.internalaudit.core.infrastructure.persistence.entity.ControlActivityJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ControlActivityRepository extends JpaRepository<ControlActivityJpaEntity, String> {
    List<ControlActivityJpaEntity> findByRiskId(String riskId);
}
