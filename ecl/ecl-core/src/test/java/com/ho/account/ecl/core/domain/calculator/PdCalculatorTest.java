package com.ho.account.ecl.core.domain.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdCalculatorTest {

    private PdCalculator pdCalculator;

    @BeforeEach
    void setUp() {
        pdCalculator = new PdCalculator();
    }

    @Test
    @DisplayName("정상 등급의 기초 PD가 PD Floor(0.0003) 이상이면 기초 PD가 적용된다.")
    void calculateAdjusted12MonthPd_Normal() {
        BigDecimal basePd = new BigDecimal("0.0100");
        BigDecimal pdFloor = new BigDecimal("0.0003");

        BigDecimal result = pdCalculator.calculateAdjusted12MonthPd(basePd, pdFloor, 0, "NORMAL");

        assertThat(result).isEqualByComparingTo(new BigDecimal("0.01000000"));
    }

    @Test
    @DisplayName("기초 PD가 극히 낮으면 PD Floor(0.0003)가 하한선으로 강제된다.")
    void calculateAdjusted12MonthPd_PdFloorApplied() {
        BigDecimal basePd = new BigDecimal("0.0001");
        BigDecimal pdFloor = new BigDecimal("0.0003");

        BigDecimal result = pdCalculator.calculateAdjusted12MonthPd(basePd, pdFloor, 0, "NORMAL");

        assertThat(result).isEqualByComparingTo(new BigDecimal("0.00030000"));
    }

    @Test
    @DisplayName("연체 30일 이상 또는 경보 발생 시 PD가 2배 할증된다.")
    void calculateAdjusted12MonthPd_PenaltyApplied() {
        BigDecimal basePd = new BigDecimal("0.0100");
        BigDecimal pdFloor = new BigDecimal("0.0003");

        BigDecimal result = pdCalculator.calculateAdjusted12MonthPd(basePd, pdFloor, 35, "NORMAL");

        assertThat(result).isEqualByComparingTo(new BigDecimal("0.02000000"));
    }

    @Test
    @DisplayName("생애 잔존 만기 3년에 대해 한계부도확률(Marginal PD) 시퀀스를 차례로 산출한다.")
    void generateMarginalPdSequence_3Years() {
        BigDecimal twelveMonthPd = new BigDecimal("0.0200"); // 2% 1년차 PD

        List<BigDecimal> sequence = pdCalculator.generateMarginalPdSequence(twelveMonthPd, 3);

        assertThat(sequence).hasSize(3);
        // Year 1 marginal = 0.02
        assertThat(sequence.get(0)).isEqualByComparingTo(new BigDecimal("0.02000000"));
        // Year 2 cum = 1 - (0.98)^2 = 1 - 0.9604 = 0.0396. marginal = (0.0396 - 0.02) / (1 - 0.02) = 0.0196 / 0.98 = 0.02
        assertThat(sequence.get(1)).isEqualByComparingTo(new BigDecimal("0.02000000"));
        // Year 3 marginal = 0.02
        assertThat(sequence.get(2)).isEqualByComparingTo(new BigDecimal("0.02000000"));
    }

    @Test
    @DisplayName("거시경제 민감도 Scaling Factor(1.35)를 적용하여 시나리오 PD를 정밀 조정한다.")
    void applyMacroeconomicFactor_PessimisticScenario() {
        BigDecimal basePd = new BigDecimal("0.02000000");
        BigDecimal scalingFactor = new BigDecimal("1.35"); // 비관 시나리오 35% 악화

        BigDecimal result = pdCalculator.applyMacroeconomicFactor(basePd, scalingFactor);

        assertThat(result).isEqualByComparingTo(new BigDecimal("0.02700000"));
    }

    @Test
    @DisplayName("전이행렬 기반 PD 곡선을 정상 산출한다.")
    void generateTransitionBasedCurve_Success() {
        com.ho.account.ecl.core.domain.model.TransitionMatrix tm = com.ho.account.ecl.core.domain.model.TransitionMatrix.builder()
                .fromRating("BBB")
                .toRating("D")
                .probability(new BigDecimal("0.0300"))
                .build();

        List<BigDecimal> curve = pdCalculator.generateTransitionBasedCurve(List.of(tm), new BigDecimal("0.0200"), 3.0);

        assertThat(curve).hasSize(3);
        assertThat(curve.get(0)).isEqualByComparingTo(new BigDecimal("0.03000000"));
    }

    @Test
    @DisplayName("전이행렬 미존재 시 단순 모델 PD 곡선을 정상 산출한다.")
    void generateSimplePdCurve_Success() {
        List<BigDecimal> curve = pdCalculator.generateSimplePdCurve(new BigDecimal("0.0500"), 3.0);

        assertThat(curve).hasSize(3);
        assertThat(curve.get(0)).isEqualByComparingTo(new BigDecimal("0.05000000"));
    }
}

