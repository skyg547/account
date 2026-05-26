package com.risk.credit.core.application.service.calculation;

import com.risk.credit.core.application.port.out.TransitionMatrixRepository;
import com.risk.credit.core.domain.model.TransitionMatrix;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [금융공학] 생애주기 부도율 곡선 생성기 (Lifetime PD Curve Generator)
 * IFRS 9 Stage 2/3 평가 시 잔여 만기 전체에 대한 다기간 부도확률인 한계부도율(Marginal PD) 목록을 생성합니다.
 *
 * 💡 [초보자를 위한 개념 설명]
 * 보통 부도율(PD)은 1년치 기준(12개월)으로 산정합니다.
 * 하지만 IFRS 9에서는 건전성이 악화된 대출에 대해 대출 실행일부터 만기일까지(생애주기) 리스크를 미리 예측해야 합니다.
 * 이 프로세스는 현재의 1년치 부도율을 바탕으로 2년차, 3년차 등 미래 각 시점의 부도 확률을 계산하여 '곡선' 형태로 만듭니다.
 *
 * 🔧 [v2.0 고도화 내역]
 * - 기존: 위험률(hazardRate) = PD 고정 (단순 지수평활 가정)
 * - 변경: 전이행렬(Transition Matrix) 기반으로 Lifetime PD 곡선 생성
 *         → 등급별로 "1년 후 부도 등급(D)으로 이동할 확률"을 누적하여 더 정밀한 곡선 산출
 * - 폴백: 전이행렬 데이터가 없으면 기존 단순 모델로 자동 전환 (안전한 Graceful Degradation)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LifetimePdService {

    /**
     * 전이행렬 저장소.
     * 💡 전이행렬이란 "AAA→AA 확률 95%, AAA→A 확률 3%, AAA→D(부도) 확률 0.01%" 같은
     *    등급 간 이동 확률표를 DB에서 관리하는 테이블입니다.
     */
    private final TransitionMatrixRepository transitionMatrixRepository;

    /**
     * 전이행렬 캐시 (Rating -> List of TransitionMatrix)
     * 💡 배치 시작 시점에 기준일에 해당하는 데이터를 일괄 로드하여 메모리에 보관합니다.
     */
    private final ConcurrentHashMap<String, List<TransitionMatrix>> tmCache = new ConcurrentHashMap<>();

    /**
     * [고도화] 배치 기준일의 모든 전이행렬 데이터를 메모리에 캐싱합니다.
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
    // 1. 고도화 Lifetime PD 곡선 생성 (전이행렬 기반)
    // ==========================================

    /**
     * [고도화] 전이행렬 기반 Lifetime PD 곡선 생성.
     *
     * 💡 [산출 원리 - 전이행렬 기반 부도 확률 추정]
     * 1단계: DB에서 해당 등급(예: BBB)의 전이 확률을 조회
     *    → "BBB에서 1년 후 각 등급으로 이동할 확률"을 알아냄
     * 2단계: 이 중 'D(부도)' 등급으로의 전이 확률이 1차년도 부도율
     * 3단계: 2차년도는 생존 확률 × 1차년도 동일 위험률로 추정
     * 4단계: 이를 만기까지 반복하여 Marginal PD 곡선 완성
     *
     * ⚠️ 전이행렬이 DB에 없으면 기존 단순 모델(generateSimplePdCurve)로 폴백합니다.
     *
     * @param initialPd12m  기초 12개월 PD (등급 마스터에서 조회한 값)
     * @param maturityYears 잔여 만기 (연 단위)
     * @param currentRating 현재 내부 신용 등급 (예: "BBB")
     * @param baseDate      전이행렬 기준 일자
     * @return 기간별 Marginal PD 리스트 (1차년도, 2차년도, ...)
     */
    public List<BigDecimal> generateMarginalPdCurve(BigDecimal initialPd12m, double maturityYears,
                                                    String currentRating, LocalDate baseDate) {
        // 전이행렬 기반 산출 시도 (캐시 활용)
        if (currentRating != null && baseDate != null) {
            List<TransitionMatrix> transitions = tmCache.get(currentRating.toUpperCase());

            // 캐시에 없는 경우 Fallback (실시간 조회)
            if (transitions == null || transitions.isEmpty()) {
                log.debug("🔍 [Lifetime PD] 캐시에 없는 등급({}) 전이행렬 실시간 조회 시도", currentRating);
                transitions = transitionMatrixRepository.findByBaseDateAndFromRating(baseDate, currentRating);
            }

            if (!transitions.isEmpty()) {
                log.debug("📈 [Lifetime PD] 전이행렬 기반 곡선 생성 - 등급: {}, 행렬 크기: {}",
                        currentRating, transitions.size());
                return generateTransitionBasedCurve(transitions, initialPd12m, maturityYears);
            }

            log.info("⚠️ [Lifetime PD] 등급 '{}'의 전이행렬이 없습니다. 단순 모델로 폴백합니다.", currentRating);
        }

        // 폴백: 기존 단순 모델
        return generateSimplePdCurve(initialPd12m, maturityYears);
    }

    /**
     * [레거시 호환] 기존 시그니처 유지 (전이행렬 미사용).
     *
     * @deprecated v2.0부터 4-파라미터 버전(currentRating, baseDate 포함)을 사용하세요.
     */
    @Deprecated(since = "2.0", forRemoval = false)
    public List<BigDecimal> generateMarginalPdCurve(BigDecimal initialPd12m, double maturityYears) {
        return generateSimplePdCurve(initialPd12m, maturityYears);
    }

    // ==========================================
    // 2. 전이행렬 기반 곡선 생성 (내부 로직)
    // ==========================================

    /**
     * 전이행렬에서 부도 등급('D')으로의 전이 확률을 추출하여 Marginal PD 곡선을 생성합니다.
     *
     * 💡 [상세 산출 과정]
     * 예를 들어 BBB 등급의 전이행렬이 다음과 같다면:
     *   BBB→AAA: 0.5%, BBB→AA: 2%, ..., BBB→D: 0.2%
     * → 1차년도 Marginal PD = 0.2% (BBB에서 직접 부도)
     * → 2차년도: 아직 살아있는(1-0.2%) 확률 × 동일 위험률 = 생존율 × 0.2%
     * → 3차년도 이후도 동일 방식 (일정 위험률 가정)
     */
    private List<BigDecimal> generateTransitionBasedCurve(List<TransitionMatrix> transitions,
                                                          BigDecimal initialPd12m,
                                                          double maturityYears) {
        // 전이행렬에서 'D(부도)' 등급으로의 전이 확률 추출
        // 💡 to_rating이 'D'인 행의 probability 값이 "이 등급에서 1년 내 부도날 확률"
        Map<String, BigDecimal> transMap = transitions.stream()
                .collect(Collectors.toMap(
                        TransitionMatrix::getToRating,
                        TransitionMatrix::getProbability,
                        (v1, v2) -> v2
                ));

        // 부도 등급으로의 전이 확률 조회 (D 등급)
        BigDecimal transitionPd = transMap.getOrDefault("D", initialPd12m);

        // 전이행렬 기반 PD와 등급 마스터 PD 중 보수적인(더 큰) 값을 사용
        // 💡 규제 관점에서 '보수적 추정'(Conservative Estimation) 원칙 적용
        double pd1 = Math.max(
                transitionPd.doubleValue(),
                (initialPd12m != null) ? initialPd12m.doubleValue() : 0.05
        );

        List<BigDecimal> curve = new ArrayList<>();

        // 1차년도 한계부도율
        curve.add(BigDecimal.valueOf(pd1).setScale(8, RoundingMode.HALF_UP));

        // 위험률(Hazard Rate) 산출
        // 💡 h = -ln(1 - PD) : PD를 연속 시간 기반 위험률로 변환
        //    이렇게 하면 누적 PD가 1을 넘지 않도록 수학적으로 보장됩니다.
        double hazardRate = -Math.log(1 - Math.min(pd1, 0.9999));

        // 다기간 Marginal PD 산출 (2차~만기)
        double cumulativePd = pd1;
        int maxYears = (int) Math.ceil(maturityYears);

        for (int t = 2; t <= maxYears; t++) {
            double survivalProb = 1.0 - cumulativePd;

            // 한계부도율 = 생존확률 × (1 - e^(-h))
            // 💡 "t-1년까지 살아남은 고객이 t년차에 부도날 확률"
            double marginalPd = survivalProb * (1 - Math.exp(-hazardRate));
            marginalPd = Math.min(marginalPd, survivalProb); // 안전 제한

            curve.add(BigDecimal.valueOf(marginalPd).setScale(8, RoundingMode.HALF_UP));
            cumulativePd += marginalPd;

            // 누적 부도확률 상한 (99% 초과 방지)
            if (cumulativePd >= 0.99) break;
        }

        log.debug("📈 [Lifetime PD] 전이행렬 기반 곡선 완성: {}개 시점, 누적PD={:.4f}",
                curve.size(), cumulativePd);

        return curve;
    }

    // ==========================================
    // 3. 단순 모델 (폴백/레거시)
    // ==========================================

    /**
     * 12개월 PD를 바탕으로 만기까지의 Marginal PD(한계부도율) 곡선을 생성합니다.
     * (단순 지수 평활 가정을 적용한 기본 모델)
     *
     * 💡 전이행렬이 없을 때 이 단순 모델이 폴백으로 사용됩니다.
     *    위험률(hazardRate) = PD로 일정하다고 가정하는 보수적인 방법입니다.
     *
     * @param initialPd12m  기초 12개월 PD
     * @param maturityYears 잔여 만기 (연 단위)
     * @return 기간별 Marginal PD 리스트
     */
    private List<BigDecimal> generateSimplePdCurve(BigDecimal initialPd12m, double maturityYears) {
        log.debug("📈 [Lifetime PD] 단순 모델 곡선 생성 - 기초 PD: {}, 잔여만기: {}년", initialPd12m, maturityYears);

        List<BigDecimal> curve = new ArrayList<>();
        double pd1 = (initialPd12m != null) ? initialPd12m.doubleValue() : 0.05;

        // 1차년도 부도율 반영
        curve.add(BigDecimal.valueOf(pd1).setScale(8, RoundingMode.HALF_UP));

        // 한계부도율 P(T) = (1 - 누적부도율(T-1)) * 위험률(HazardRate)
        double cumulativePd = pd1;
        double hazardRate = pd1; // 단순 산출을 위해 위험률이 기간 동안 일정하다는 가정

        int maxYears = (int) Math.ceil(maturityYears);
        for (int t = 2; t <= maxYears; t++) {
            double survivalProb = 1.0 - cumulativePd;
            double marginalPd = Math.min(survivalProb * hazardRate, survivalProb);

            curve.add(BigDecimal.valueOf(marginalPd).setScale(8, RoundingMode.HALF_UP));
            cumulativePd += marginalPd;

            if (cumulativePd >= 0.99)
                break; // 누적 부도확률에 대한 상한 (99% 초과 방지)
        }

        return curve;
    }
}
