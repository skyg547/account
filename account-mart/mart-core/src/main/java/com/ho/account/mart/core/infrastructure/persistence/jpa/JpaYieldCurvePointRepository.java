package com.ho.account.mart.core.infrastructure.persistence.jpa;

import com.ho.account.mart.core.infrastructure.persistence.entity.marketdata.YieldCurvePointEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaYieldCurvePointRepository extends JpaRepository<YieldCurvePointEntity, Long> {
}
