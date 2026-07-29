package com.ho.account.internalaudit.core.infrastructure.persistence.repository;

import com.ho.account.internalaudit.core.infrastructure.persistence.entity.RcmRiskJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RcmRiskRepository extends JpaRepository<RcmRiskJpaEntity, String> {
    List<RcmRiskJpaEntity> findByProcessId(String processId);
}
