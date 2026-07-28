package com.ho.account.internalaudit.core.infrastructure.persistence.repository;

import com.ho.account.internalaudit.core.infrastructure.persistence.entity.RcmProcessJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RcmProcessRepository extends JpaRepository<RcmProcessJpaEntity, String> {
}
