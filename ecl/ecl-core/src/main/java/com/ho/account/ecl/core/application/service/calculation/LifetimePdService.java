package com.ho.account.ecl.core.application.service.calculation;

import com.ho.account.ecl.core.application.port.out.TransitionMatrixRepository;
import com.ho.account.ecl.core.domain.calculator.PdCalculator;
import com.ho.account.ecl.core.domain.model.TransitionMatrix;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * [애플리케이션 서비스] 생애주기 부도율 곡선 생성 조율자 (Lifetime PD Curve Application Service)
 *
 * 💡 [DDD & Hexagonal Architecture 설계 원칙]
 * 1. Application Service의 역할 (Orchestration):
 *    본 서비스는 DB/캐시에서 전이행렬(Transition Matrix) 데이터를 로드 및 캐싱하고,
 *    도메인 계산기({@link PdCalculator})를 호출하여 PD 곡선을 생성하도록 프로세스를 조율(Orchestrate)합니다.
 *
 * 2. Domain Calculator로의 핵심 계산 이관 (Encapsulation):
 *    기존 서비스에 위치하던 전이행렬 부도 확률 추출, 연속 위험률(Hazard Rate) 연산, 다기간 한계부도율(Marginal PD)
 *    지수 평활 산식 등의 핵심 수학/금융 로직은 전부 Pure Domain Calculator인 {@link PdCalculator}로 이관되었습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LifetimePdService {

    /** 전이행렬 저장소 포트 */
    private final TransitionMatrixRepository transitionMatrixRepository;

    /** Pure Domain Calculator */
    private final PdCalculator pdCalculator;

    /** 전이행렬 메모리 캐시 (Rating -> List of TransitionMatrix) */
    private final ConcurrentHashMap<String, List<TransitionMatrix>> tmCache = new ConcurrentHashMap<>();

    /**
     * [애플리케이션 서비스] 배치 기준일의 전이행렬 데이터를 캐싱합니다.
     * @param baseDate 배치 기준일
     */
    public void refreshCache(LocalDate baseDate) {
        log.info("📦 [Lifetime PD] 기준일({}) 전이행렬 캐시 갱신 중...", baseDate);
        List<TransitionMatrix> allTransitions = transitionMatrixRepository.findByBaseDate(baseDate);
        
        tmCache.clear();
        tmCache.putAll(allTransitions.stream()
                .collect(Collectors.groupingBy(TransitionMatrix::getFromRating)));
        
        log.info("✅ [Lifetime PD] 총 {}개 등급의 전이행렬 데이터가 캐싱되었습니다.", tmCache.size());
    }

    // ==========================================
    // Lifetime PD 곡선 생성 (Orchestration)
    // ==========================================

    /**
     * [애플리케이션 서비스] 전이행렬 기반 Lifetime PD 곡선 생성 프로세스 조율.
     *
     * 💡 [조율 흐름]
     * 1단계: 캐시 또는 Repository에서 해당 등급의 전이행렬 데이터를 조회 (Data Fetching)
     * 2단계: 데이터 존재 시 Pure Domain Calculator({@link PdCalculator#generateTransitionBasedCurve}) 호출
     * 3단계: 미존재 시 단순 모델 Pure Domain Calculator({@link PdCalculator#generateSimplePdCurve}) 폴백 호출
     *
     * @param initialPd12m  기초 12개월 PD
     * @param maturityYears 잔여 만기 (연 단위)
     * @param currentRating 현재 내부 신용 등급 (예: "BBB")
     * @param baseDate      전이행렬 기준 일자
     * @return 기간별 Marginal PD 리스트 (1차년도, 2차년도, ...)
     */
    public List<BigDecimal> generateMarginalPdCurve(BigDecimal initialPd12m, double maturityYears,
                                                    String currentRating, LocalDate baseDate) {
        if (currentRating != null && baseDate != null) {
            List<TransitionMatrix> transitions = tmCache.get(currentRating.toUpperCase());

            if (transitions == null || transitions.isEmpty()) {
                log.debug("🔍 [Lifetime PD] 캐시에 없는 등급({}) 전이행렬 실시간 조회 시도", currentRating);
                transitions = transitionMatrixRepository.findByBaseDateAndFromRating(baseDate, currentRating);
            }

            if (!transitions.isEmpty()) {
                log.debug("📈 [Lifetime PD] Domain Calculator 전이행렬 기반 곡선 생성 호출 - 등급: {}", currentRating);
                return pdCalculator.generateTransitionBasedCurve(transitions, initialPd12m, maturityYears);
            }

            log.info("⚠️ [Lifetime PD] 등급 '{}'의 전이행렬이 없습니다. 단순 모델로 폴백합니다.", currentRating);
        }

        return pdCalculator.generateSimplePdCurve(initialPd12m, maturityYears);
    }

    /**
     * [레거시 호환] 기존 시그니처 유지 (전이행렬 미사용).
     *
     * @deprecated v2.0부터 4-파라미터 버전(currentRating, baseDate 포함)을 사용하세요.
     */
    @Deprecated(since = "2.0", forRemoval = false)
    public List<BigDecimal> generateMarginalPdCurve(BigDecimal initialPd12m, double maturityYears) {
        return pdCalculator.generateSimplePdCurve(initialPd12m, maturityYears);
    }
}



