package com.risk.mart.core.infrastructure.persistence.jpa;

import com.risk.mart.core.infrastructure.persistence.entity.ods.OdsAccountRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaOdsAccountRateRepository extends JpaRepository<OdsAccountRateEntity, String> {
}
