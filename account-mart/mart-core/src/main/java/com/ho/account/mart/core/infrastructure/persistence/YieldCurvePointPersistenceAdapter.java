package com.ho.account.mart.core.infrastructure.persistence;

import com.ho.account.mart.core.application.port.out.YieldCurvePointRepository;
import com.ho.account.mart.core.domain.marketdata.YieldCurvePoint;
import com.ho.account.mart.core.infrastructure.persistence.entity.marketdata.YieldCurveEntity;
import com.ho.account.mart.core.infrastructure.persistence.entity.marketdata.YieldCurvePointEntity;
import com.ho.account.mart.core.infrastructure.persistence.jpa.JpaYieldCurvePointRepository;
import com.ho.account.mart.core.infrastructure.persistence.jpa.JpaYieldCurveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class YieldCurvePointPersistenceAdapter implements YieldCurvePointRepository {

    private final JpaYieldCurvePointRepository jpaRepository;
    private final JpaYieldCurveRepository curveRepository;

    @Override
    public List<YieldCurvePoint> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::main)
                .collect(Collectors.toList());
    }

    @Override
    public YieldCurvePoint save(YieldCurvePoint point) {
        return main(jpaRepository.save(toEntity(point)));
    }

    @Override
    public void saveAll(List<YieldCurvePoint> points) {
        if (points == null) {
            return;
        }
        jpaRepository.saveAll(points.stream()
                .map(this::toEntity)
                .collect(Collectors.toList()));
    }

    private YieldCurvePoint main(YieldCurvePointEntity entity) {
        if (entity == null) {
            return null;
        }
        return YieldCurvePoint.builder()
                .tenorCode(entity.getTenorLabel())
                .tenorYear(entity.getTenorMonths() == null ? null : entity.getTenorMonths() / 12.0d)
                .rateValue(entity.getRate())
                .build();
    }

    private YieldCurvePointEntity toEntity(YieldCurvePoint domain) {
        if (domain == null) {
            return null;
        }
        Integer tenorMonths = domain.getTenorYear() == null ? null : (int) Math.round(domain.getTenorYear() * 12.0d);
        YieldCurvePointEntity entity = YieldCurvePointEntity.builder()
                .tenorLabel(domain.getTenorCode())
                .tenorMonths(tenorMonths)
                .rate(domain.getRateValue())
                .build();
        YieldCurveEntity curve = curveRepository
                .findByCurveNameAndBaseDate(domain.getCurveName(), domain.getBaseDate())
                .orElse(null);
        entity.setYieldCurve(curve);
        return entity;
    }
}
