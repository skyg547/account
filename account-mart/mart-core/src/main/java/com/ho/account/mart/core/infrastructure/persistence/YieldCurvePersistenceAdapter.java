package com.ho.account.mart.core.infrastructure.persistence;

import com.ho.account.shared.finance.enums.CurrencyCode;
import com.ho.account.mart.core.application.port.out.YieldCurveRepository;
import com.ho.account.mart.core.domain.marketdata.YieldCurve;
import com.ho.account.mart.core.domain.marketdata.YieldCurvePoint;
import com.ho.account.mart.core.infrastructure.persistence.entity.marketdata.YieldCurveEntity;
import com.ho.account.mart.core.infrastructure.persistence.entity.marketdata.YieldCurvePointEntity;
import com.ho.account.mart.core.infrastructure.persistence.jpa.JpaYieldCurveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class YieldCurvePersistenceAdapter implements YieldCurveRepository {

    private final JpaYieldCurveRepository jpaRepository;

    @Override
    public Optional<YieldCurve> findByCurveNameAndBaseDate(String curveName, LocalDate baseDate) {
        return jpaRepository.findByCurveNameAndBaseDate(curveName, baseDate)
                .map(this::main);
    }

    @Override
    public List<YieldCurve> findByBaseDate(LocalDate baseDate) {
        return jpaRepository.findByBaseDate(baseDate).stream()
                .map(this::main)
                .collect(Collectors.toList());
    }

    @Override
    public List<YieldCurve> findByCurrency(String currency) {
        CurrencyCode currencyCode = CurrencyCode.valueOf(currency);
        return jpaRepository.findByCurrency(currencyCode).stream()
                .map(this::main)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<YieldCurve> findLatestByCurveName(String curveName) {
        return jpaRepository.findTopByCurveNameOrderByBaseDateDesc(curveName)
                .map(this::main);
    }

    @Override
    public YieldCurve save(YieldCurve yieldCurve) {
        return main(jpaRepository.save(toEntity(yieldCurve)));
    }

    private YieldCurve main(YieldCurveEntity entity) {
        if (entity == null) {
            return null;
        }
        List<YieldCurvePoint> points = entity.getPoints() == null ? List.of() : entity.getPoints().stream()
                .map(this::toPointDomain)
                .collect(Collectors.toList());
        return YieldCurve.builder()
                .curveName(entity.getCurveName())
                .baseDate(entity.getBaseDate())
                .currency(entity.getCurrency() == null ? null : entity.getCurrency().name())
                .curveDescription(entity.getDescription())
                .points(points)
                .build();
    }

    private YieldCurvePoint toPointDomain(YieldCurvePointEntity entity) {
        if (entity == null) {
            return null;
        }
        return YieldCurvePoint.builder()
                .tenorCode(entity.getTenorLabel())
                .tenorYear(entity.getTenorMonths() == null ? null : entity.getTenorMonths() / 12.0d)
                .rateValue(entity.getRate())
                .build();
    }

    private YieldCurveEntity toEntity(YieldCurve domain) {
        if (domain == null) {
            return null;
        }
        CurrencyCode currencyCode = domain.getCurrency() == null ? null : CurrencyCode.valueOf(domain.getCurrency());
        return YieldCurveEntity.builder()
                .curveName(domain.getCurveName())
                .baseDate(domain.getBaseDate())
                .currency(currencyCode)
                .description(domain.getCurveDescription())
                .isActive(true)
                .build();
    }
}
