package com.ho.account.mart.core.domain.marketdata.service;

import com.ho.account.mart.core.domain.marketdata.YieldCurve;
import com.ho.account.mart.core.domain.marketdata.YieldCurvePoint;
import com.ho.account.mart.core.application.port.out.YieldCurveRepository;
import com.ho.account.mart.core.application.port.out.YieldCurvePointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * [시장 데이터 서비스] 수익률 곡선(Yield Curve) 관리 서비스
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class YieldCurveService {

    private final YieldCurveRepository repository;
    private final YieldCurvePointRepository pointRepository;

    /**
     * 특정 곡선의 시점별 금리 데이터를 생성 및 저장한다.
     */
    public YieldCurve createCurve(YieldCurve curve, List<YieldCurvePoint> points) {
        log.info("📈 수익률 곡선 생성 시작: {} (기준일: {})", curve.getCurveName(), curve.getBaseDate());
        
        YieldCurve saved = repository.save(curve);
        
        points.forEach(point -> {
            point.setCurveName(saved.getCurveName());
            point.setBaseDate(saved.getBaseDate());
        });
        pointRepository.saveAll(points);
        
        return saved;
    }

    /**
     * 특정 곡선의 정보를 조회한다.
     */
    @Transactional(readOnly = true)
    public Optional<YieldCurve> getCurve(String curveName, LocalDate baseDate) {
        return repository.findByCurveNameAndBaseDate(curveName, baseDate);
    }

    @Transactional(readOnly = true)
    public List<YieldCurve> getCurvesByDate(LocalDate baseDate) {
        return repository.findByBaseDate(baseDate);
    }

    @Transactional(readOnly = true)
    public List<YieldCurve> getCurvesByCurrency(String currency) {
        return repository.findByCurrency(currency);
    }

    @Transactional(readOnly = true)
    public Optional<YieldCurve> getLatestCurve(String curveName) {
        return repository.findLatestByCurveName(curveName);
    }
}
