package com.ho.account.ecl.core.domain.calculator;

import com.ho.account.shared.finance.enums.CrStaging;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

/**
 * [도메인 계산기] IFRS 9 기대신용손실(ECL) 정밀 계산기.
 *
 * 💡 [초보자를 위한 개념 및 수학 공식 설명]
 * IFRS 9 (국제회계기준)에서는 대출 자산의 예상 손실(ECL)을 다음 3가지 핵심 축의 곱과 현가 할인으로 계산합니다:
 * 1. PD (Probability of Default): 부도 확률 (0 ~ 1.0)
 * 2. LGD (Loss Given Default): 부도 시 손실률 (0 ~ 1.0)
 * 3. EAD (Exposure at Default): 부도 시 익스포저 (원금 및 한도)
 * 4. Discount Factor (할인요소): 현금흐름의 시간가치를 반영하기 위한 현가 할인 계수 \( \frac{1}{(1 + r)^t} \)
 *
 * 📌 [Stage별 계산 방식]
 * - Stage 1 (정상): 향후 12개월간 발생할 수 있는 12개월 기대신용손실(12-month ECL)을 산출합니다.
 *   \( ECL_{12m} = PD_{12m} \times LGD \times EAD \times \frac{1}{1 + r} \)
 * - Stage 2 & 3 (주의/손상): 자산의 전체 잔존 만기 동안 발생할 수 있는 생애 기대신용손실(Lifetime ECL)을 산출합니다.
 *   \( ECL_{Lifetime} = \sum_{t=1}^{T} \left( MarginalPD_t \times LGD \times EAD \times \frac{1}{(1 + r)^t} \right) \)
 *
 * 🔧 [금융 정밀도 정책]
 * - 부동소수점(`double`/`float`) 사용 금지: `Math.pow` 대신 순수 `BigDecimal.pow()` 및 `divide()` 사용
 * - 중간 연산 정밀도: `MathContext(15, RoundingMode.HALF_UP)`
 * - 최종 결과 스케일: 소수점 4자리 반올림 (`setScale(4, RoundingMode.HALF_UP)`)
 */
@Component
public class IfrsEclCalculator {

    /** 금융 연산용 15자리 내부 정밀도 Context */
    private static final MathContext MC = new MathContext(15, RoundingMode.HALF_UP);

    /** 기본 할인율 (입력 누락 시 5% 적용) */
    private static final BigDecimal DEFAULT_DISCOUNT_RATE = new BigDecimal("0.05");

    /**
     * 단일 거시경제 시나리오 하에서의 IFRS 9 ECL(기대신용손실)을 산출합니다.
     *
     * @param stage        IFRS 9 자산 단계 (STAGE1, STAGE2, STAGE3)
     * @param marginalPds  각 연도별 한계부도확률(Marginal PD) 리스트 (t=0: 1년차, t=1: 2년차, ...)
     * @param lgd          부도 시 손실률 (LGD, 0 ~ 1.0)
     * @param ead          부도 시 익스포저 (EAD, 원화 금액)
     * @param discountRate 할인율 (유효이자율 r, 예: 0.05 = 5%)
     * @return 순수 BigDecimal로 정밀 산출된 ECL 금액 (소수점 4자리)
     */
    public BigDecimal calculateEcl(
            CrStaging stage,
            List<BigDecimal> marginalPds,
            BigDecimal lgd,
            BigDecimal ead,
            BigDecimal discountRate) {

        if (marginalPds == null || marginalPds.isEmpty() || lgd == null || ead == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal safeDiscountRate = (discountRate != null && discountRate.compareTo(BigDecimal.ZERO) >= 0)
                ? discountRate
                : DEFAULT_DISCOUNT_RATE;

        // 1. Stage 1 (12개월 기대신용손실)
        if (stage == CrStaging.STAGE1) {
            BigDecimal twelveMonthPd = marginalPds.get(0);
            BigDecimal discountFactor = calculateDiscountFactor(safeDiscountRate, 1);

            return twelveMonthPd
                    .multiply(lgd, MC)
                    .multiply(ead, MC)
                    .multiply(discountFactor, MC)
                    .setScale(4, RoundingMode.HALF_UP);
        }

        // 2. Stage 2 & Stage 3 (생애 기대신용손실 Lifetime ECL)
        BigDecimal totalLifetimeEcl = BigDecimal.ZERO;

        for (int t = 0; t < marginalPds.size(); t++) {
            BigDecimal marginalPd = marginalPds.get(t);
            int yearPeriod = t + 1; // 1년차, 2년차...
            BigDecimal discountFactor = calculateDiscountFactor(safeDiscountRate, yearPeriod);

            BigDecimal periodEcl = marginalPd
                    .multiply(lgd, MC)
                    .multiply(ead, MC)
                    .multiply(discountFactor, MC);

            totalLifetimeEcl = totalLifetimeEcl.add(periodEcl, MC);
        }

        return totalLifetimeEcl.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 다중 거시경제 시나리오(낙관/중립/비관) 가중평균 ECL을 산출합니다.
     *
     * 💡 [IFRS 9 다중 시나리오 평가]
     * 회계기준서에 따라 미래 전망(Forward-Looking Information)을 반영하기 위해
     * 최소 3가지 거시경제 시나리오별 ECL을 구한 뒤, 가중치(\( \sum w_i = 1.0 \))를 적용해 합산합니다.
     *
     * @param optimisticEcl 낙관 시나리오 산출 ECL
     * @param baselineEcl   중립(기본) 시나리오 산출 ECL
     * @param pessimisticEcl 비관 시나리오 산출 ECL
     * @param optWeight     낙관 시나리오 가중치 (예: 0.20)
     * @param baseWeight    중립 시나리오 가중치 (예: 0.60)
     * @param pessWeight    비관 시나리오 가중치 (예: 0.20)
     * @return 가중평균 최종 ECL
     */
    public BigDecimal calculateWeightedEcl(
            BigDecimal optimisticEcl,
            BigDecimal baselineEcl,
            BigDecimal pessimisticEcl,
            BigDecimal optWeight,
            BigDecimal baseWeight,
            BigDecimal pessWeight) {

        BigDecimal safeOpt = optimisticEcl != null ? optimisticEcl : BigDecimal.ZERO;
        BigDecimal safeBase = baselineEcl != null ? baselineEcl : BigDecimal.ZERO;
        BigDecimal safePess = pessimisticEcl != null ? pessimisticEcl : BigDecimal.ZERO;

        BigDecimal safeOptW = optWeight != null ? optWeight : new BigDecimal("0.20");
        BigDecimal safeBaseW = baseWeight != null ? baseWeight : new BigDecimal("0.60");
        BigDecimal safePessW = pessWeight != null ? pessWeight : new BigDecimal("0.20");

        BigDecimal weightedOpt = safeOpt.multiply(safeOptW, MC);
        BigDecimal weightedBase = safeBase.multiply(safeBaseW, MC);
        BigDecimal weightedPess = safePess.multiply(safePessW, MC);

        return weightedOpt
                .add(weightedBase, MC)
                .add(weightedPess, MC)
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 순수 BigDecimal 기반의 현가 할인 계수(Discount Factor) 계산.
     * \( DF_t = \frac{1}{(1 + r)^t} \)
     *
     * @param discountRate 할인율 r (예: 0.05)
     * @param period       경과 기간 t (연단위, 1, 2, 3...)
     * @return 정밀 할인 계수
     */
    private BigDecimal calculateDiscountFactor(BigDecimal discountRate, int period) {
        BigDecimal base = BigDecimal.ONE.add(discountRate, MC);
        BigDecimal compoundBase = base.pow(period, MC);
        return BigDecimal.ONE.divide(compoundBase, MC);
    }
}
