package com.ho.account.mart.core.infrastructure.persistence.jpa;

import com.ho.account.shared.finance.enums.CurrencyCode;
import com.ho.account.mart.core.infrastructure.persistence.entity.marketdata.YieldCurveEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JpaYieldCurveRepository extends JpaRepository<YieldCurveEntity, Long> {
    Optional<YieldCurveEntity> findByCurveNameAndBaseDate(String curveName, LocalDate baseDate);

    List<YieldCurveEntity> findByBaseDate(LocalDate baseDate);

    List<YieldCurveEntity> findByCurrency(CurrencyCode currency);

    Optional<YieldCurveEntity> findTopByCurveNameOrderByBaseDateDesc(String curveName);
}
