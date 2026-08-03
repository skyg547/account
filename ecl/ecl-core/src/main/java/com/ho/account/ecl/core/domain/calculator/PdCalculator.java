package com.ho.account.ecl.core.domain.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * [도메인 계산기] IFRS 9 PD(Probability of Default, 부도확률) 정밀 계산 도메인 모델.
 *
 * 💡 [초보자를 위한 개념 설명]
 * - PD Floor (부도확률 하한선): 자산 신용등급이 우량해도 회계/감독 규정에 따라 최소한으로 적용해야 하는 바닥 PD 수치입니다. (예: 0.0003 = 0.03%)
 * - Delinquency/Warning Penalty (동적 위험 할증): 연체 30일 이상이거나 경보 발생 시 부도 확률을 2배 이상 할증합니다.
 * - Lifetime Cumulative PD -> Marginal PD 분해:
 *   누적 부도확률 $CumPD_t$ 로부터 각 연도별 순수 부도확률인 한계 부도확률 $MarginalPD_t$ 를 계산합니다.
 *   $MarginalPD_t = \frac{CumPD_t - CumPD_{t-1}}{1 - CumPD_{t-1}}$
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
