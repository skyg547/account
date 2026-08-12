package com.ho.account.ecl.core.domain.calculator;

import com.ho.account.ecl.core.domain.model.TransitionMatrix;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * [도메인 계산기] IFRS 9 PD(Probability of Default, 부도확률) 정밀 계산 순수 도메인 모델 (Pure Domain Calculator).
 *
 * 💡 [초보자를 위한 개념 설명 & DDD 설계 이유]
 * 1. Rich Domain Model vs Anemic Service:
 *    기존에는 애플리케이션 서비스(LifetimePdService) 내부에 연속 위험률(Hazard Rate) 연산 및
 *    전이행렬(Transition Matrix) 기반 다기간 한계부도율(Marginal PD) 산식 등의 핵심 도메인 로직이 유출되어 있었습니다.
 *    이를 순수 도메인 계산기(PdCalculator)로 캡슐화하여 서비스의 역할을 데이터 조회 및 조율(Orchestration)로 한정합니다.
 *
 * 2. 부도확률 주요 공식:
 *    - PD Floor (하한선): 신용 우량 차주라도 회계 규정에 따른 최소 적용 PD (예: 0.03%)
 *    - 연속 위험률: $h = -\ln(1 - \min(pd_1, 0.9999))$ (누적 PD가 1을 넘지 않도록 안전 보장)
 *    - 연도 $t$의 한계부도율: $MarginalPD_t = (1 - CumPD_{t-1}) \times (1 - e^{-h})$
 */
@Component
public class PdCalculator {

    private static final MathContext MC = new MathContext(15, RoundingMode.HALF_UP);

    /**
     * 기초 12개월 PD에 위험 할증(Penalty) 및 PD Floor 규제를 반영하여 최종 보정 PD를 산출합니다.
     *
     * @param basePd          기초 신용 등급 PD (예: 0.0100)
     * @param pdFloor         모델 파라미터 PD 하한선 (예: 0.0003)
     * @param delinquentDays  연체 일수
     * @param warningLevel    조기경보 등급 ("NORMAL", "WARNING", "CRITICAL")
     * @return 보정 및 Floor 적용 완료된 최종 12개월 PD (소수점 8자리)
     */
    public BigDecimal calculateAdjusted12MonthPd(
            BigDecimal basePd,
            BigDecimal pdFloor,
            int delinquentDays,
            String warningLevel) {

        BigDecimal safeBasePd = (basePd != null) ? basePd : BigDecimal.ZERO;
        BigDecimal safePdFloor = (pdFloor != null) ? pdFloor : new BigDecimal("0.0003");
        String safeWarningLevel = (warningLevel != null) ? warningLevel.toUpperCase() : "NORMAL";

        BigDecimal penalizedPd = safeBasePd;

        // 1. 연체일수 30일 이상 또는 조기경보 발령 시 PD 2배 할증
        if (delinquentDays >= 30 || "WARNING".equals(safeWarningLevel) || "CRITICAL".equals(safeWarningLevel)) {
            penalizedPd = safeBasePd.multiply(new BigDecimal("2.0"), MC)
                    .max(safePdFloor.multiply(new BigDecimal("10.0"), MC));
        }

        // 2. 최종 PD Floor 반영 (PD는 1.0을 초과할 수 없음)
        BigDecimal finalPd = penalizedPd.max(safePdFloor);
        if (finalPd.compareTo(BigDecimal.ONE) > 0) {
            finalPd = BigDecimal.ONE;
        }

        return finalPd.setScale(8, RoundingMode.HALF_UP);
    }

    /**
     * 12개월 1차년도 PD와 경과 연수를 바탕으로 생애 한계부도확률(Marginal PD) 시퀀스를 생성합니다.
     *
     * 💡 [한계 PD 계산 공식]
     * 연도 $t$의 누적부도확률을 $CumPD_t = 1 - (1 - PD_{12m})^t$ 로 추정할 때,
     * 각 연도의 한계부도확률 $MarginalPD_t$ 는 생존한 차주 중에서 해당 연도에 새로 부도날 확률입니다.
     *
     * @param twelveMonthPd 1차년도 보정 12개월 PD
     * @param lifetimeYears 생애 잔존 만기 (연 단위, 예: 3년)
     * @return 각 연도별 한계 PD 리스트 [MarginalPD_1, MarginalPD_2, ...]
     */
    public List<BigDecimal> generateMarginalPdSequence(BigDecimal twelveMonthPd, int lifetimeYears) {
        if (twelveMonthPd == null || twelveMonthPd.compareTo(BigDecimal.ZERO) <= 0) {
            return Collections.singletonList(BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP));
        }

        int years = Math.max(1, lifetimeYears);
        List<BigDecimal> marginalPds = new ArrayList<>();

        BigDecimal previousCumPd = BigDecimal.ZERO;

        for (int t = 1; t <= years; t++) {
            // CumPD_t = 1 - (1 - PD_12m)^t
            BigDecimal survivalBase = BigDecimal.ONE.subtract(twelveMonthPd, MC);
            BigDecimal cumSurvival = survivalBase.pow(t, MC);
            BigDecimal currentCumPd = BigDecimal.ONE.subtract(cumSurvival, MC);

            // MarginalPD_t = (CumPD_t - CumPD_{t-1}) / (1 - CumPD_{t-1})
            BigDecimal num = currentCumPd.subtract(previousCumPd, MC);
            BigDecimal den = BigDecimal.ONE.subtract(previousCumPd, MC);

            BigDecimal marginalPd = BigDecimal.ZERO;
            if (den.compareTo(BigDecimal.ZERO) > 0) {
                marginalPd = num.divide(den, MC);
            }

            marginalPds.add(marginalPd.setScale(8, RoundingMode.HALF_UP));
            previousCumPd = currentCumPd;
        }

        return marginalPds;
    }

    /**
     * [Pure Domain Calculator] 전이행렬(Transition Matrix)에서 부도 등급('D')으로의 전이 확률을 추출하여 Marginal PD 곡선을 생성합니다.
     *
     * 💡 [금융공학 산출 원리 & pure BigDecimal 정밀도 제어]
     * 1. 1차년도 PD($pd_1$): 전이행렬 상의 D(Default) 등급 전이 확률과 등급 마스터 기초 PD 중 보수적인(더 큰) 수치 선택
     * 2. 연속 위험률(Hazard Rate, $h$) 및 한계부도율 산식:
     *    - 부부확률을 연속 시간 모델로 표현하면 $h = -\ln(1 - pd_1)$ 입니다.
     *    - 이때 연도별 한계부도율 $mPD_t = survivalProb_{t-1} \times (1 - e^{-h})$ 가 되는데,
     *      수학적 등가성에 의해 $1 - e^{-h} = 1 - e^{\ln(1 - pd_1)} = 1 - (1 - pd_1) = pd_1$ 이 성립합니다!
     *    - 따라서 부동소수점 함수인 `Math.log` 및 `Math.exp` 사용 시 발생하는 IEEE 754 부동소수점 오차(Floating-point Precision Loss)를
     *      전면 차단하고, Pure `BigDecimal` 및 `MathContext(15, RoundingMode.HALF_UP)` 만으로 100% 정밀하게 연산합니다.
     * 3. IFRS 9 손실충당금 관점의 이점:
     *    - 생애 잔존 만기 동안 부도확률 누적 오차가 차단되어 자산 스테이징 및 대손충당금 평가의 회계적 신뢰성을 보장합니다.
     *
     * @param transitions 해당 신용등급의 전이행렬 엔티티 목록
     * @param initialPd12m 기초 12개월 PD
     * @param maturityYears 잔여만기 (연 단위, BigDecimal)
     * @return 연도별 Marginal PD 곡선 리스트 (소수점 8자리 정밀도)
     */
    public List<BigDecimal> generateTransitionBasedCurve(
            List<TransitionMatrix> transitions,
            BigDecimal initialPd12m,
            BigDecimal maturityYears) {

        Map<String, BigDecimal> transMap = transitions.stream()
                .collect(Collectors.toMap(
                        TransitionMatrix::getToRating,
                        TransitionMatrix::getProbability,
                        (v1, v2) -> v2
                ));

        BigDecimal transitionPd = transMap.getOrDefault("D", initialPd12m);

        BigDecimal basePd1 = (transitionPd != null) ? transitionPd : new BigDecimal("0.05");
        BigDecimal safeInitPd = (initialPd12m != null) ? initialPd12m : new BigDecimal("0.05");
        BigDecimal pd1 = basePd1.max(safeInitPd);

        List<BigDecimal> curve = new ArrayList<>();
        curve.add(pd1.setScale(8, RoundingMode.HALF_UP));

        BigDecimal cumulativePd = pd1;
        int maxYears = (maturityYears != null) ? maturityYears.setScale(0, RoundingMode.CEILING).intValue() : 1;
        maxYears = Math.max(1, maxYears);

        for (int t = 2; t <= maxYears; t++) {
            // survivalProb = 1.0 - cumulativePd
            BigDecimal survivalProb = BigDecimal.ONE.subtract(cumulativePd, MC);
            if (survivalProb.compareTo(BigDecimal.ZERO) <= 0) {
                curve.add(BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP));
                continue;
            }

            // marginalPd = survivalProb * pd1 (수학적 등가성에 의해 1 - exp(-h) = pd1)
            BigDecimal marginalPd = survivalProb.multiply(pd1, MC).min(survivalProb);

            curve.add(marginalPd.setScale(8, RoundingMode.HALF_UP));
            cumulativePd = cumulativePd.add(marginalPd, MC);

            if (cumulativePd.compareTo(new BigDecimal("0.99")) >= 0) break;
        }

        return curve;
    }

    /**
     * [Pure Domain Calculator] 전이행렬 미존재 시 단순 지수 평활 가정을 적용하여 Marginal PD 곡선을 생성합니다.
     *
     * 💡 [단순 모델 산식 & 부동소수점 제거 설명]
     * - $t$년차 한계부도율 = $\min(\text{생존확률} \times \text{HazardRate}, \text{생존확률})$
     * - 부동소수점 primitive `double` 대신 `BigDecimal`과 `MathContext(15, RoundingMode.HALF_UP)`를 사용하여
     *   누적 부도확률 계산 시 1e-16 수준의 근사 오차가 금융 통계 손손실액 평가액에 왜곡을 주는 위험을 원천 차단합니다.
     *
     * @param initialPd12m 기초 12개월 PD
     * @param maturityYears 잔여만기 (연 단위, BigDecimal)
     * @return 연도별 Marginal PD 곡선 리스트 (소수점 8자리 정밀도)
     */
    public List<BigDecimal> generateSimplePdCurve(BigDecimal initialPd12m, BigDecimal maturityYears) {
        List<BigDecimal> curve = new ArrayList<>();
        BigDecimal pd1 = (initialPd12m != null) ? initialPd12m : new BigDecimal("0.05");

        curve.add(pd1.setScale(8, RoundingMode.HALF_UP));

        BigDecimal cumulativePd = pd1;
        BigDecimal hazardRate = pd1;

        int maxYears = (maturityYears != null) ? maturityYears.setScale(0, RoundingMode.CEILING).intValue() : 1;
        maxYears = Math.max(1, maxYears);

        for (int t = 2; t <= maxYears; t++) {
            BigDecimal survivalProb = BigDecimal.ONE.subtract(cumulativePd, MC);
            if (survivalProb.compareTo(BigDecimal.ZERO) <= 0) {
                curve.add(BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP));
                continue;
            }

            BigDecimal rawMarginalPd = survivalProb.multiply(hazardRate, MC);
            BigDecimal marginalPd = rawMarginalPd.min(survivalProb);

            curve.add(marginalPd.setScale(8, RoundingMode.HALF_UP));
            cumulativePd = cumulativePd.add(marginalPd, MC);

            if (cumulativePd.compareTo(new BigDecimal("0.99")) >= 0) break;
        }

        return curve;
    }

    /**
     * 거시경제 민감도 계수(Macro Scaling Factor)를 적용하여 시나리오별 PD를 조정합니다.
     *
     * @param basePd        기초 PD
     * @param scalingFactor 거시경제 민감도 (예: 낙관=0.85, 중립=1.00, 비관=1.35)
     * @return 거시경제 시나리오 보정 PD
     */
    public BigDecimal applyMacroeconomicFactor(BigDecimal basePd, BigDecimal scalingFactor) {
        if (basePd == null) return BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP);
        BigDecimal factor = (scalingFactor != null) ? scalingFactor : BigDecimal.ONE;

        BigDecimal adjusted = basePd.multiply(factor, MC);
        if (adjusted.compareTo(BigDecimal.ONE) > 0) {
            adjusted = BigDecimal.ONE;
        }
        return adjusted.setScale(8, RoundingMode.HALF_UP);
    }
}

