package com.ho.account.ecl.core.domain.calculator;

import com.ho.account.shared.finance.enums.CrStaging;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IfrsEclCalculatorTest {

    private IfrsEclCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new IfrsEclCalculator();
    }

    @Test
    @DisplayName("Stage 1 (12개월 ECL): 1차년도 PD, LGD, EAD 및 현가할인을 정밀 산출한다.")
    void calculateEcl_Stage1_12MonthEcl() {
        // PD = 0.02 (2%), LGD = 0.45 (45%), EAD = 100,000,000, Discount = 0.05 (5%)
        // ECL = 0.02 * 0.45 * 100,000,000 / 1.05 = 900,000 / 1.05 = 857,142.857142... -> 857142.8571
        List<BigDecimal> pds = Collections.singletonList(new BigDecimal("0.02"));
        BigDecimal lgd = new BigDecimal("0.45");
        BigDecimal ead = new BigDecimal("100000000.0000");
        BigDecimal discountRate = new BigDecimal("0.05");

        BigDecimal result = calculator.calculateEcl(CrStaging.STAGE1, pds, lgd, ead, discountRate);

        assertThat(result).isEqualByComparingTo(new BigDecimal("857142.8571"));
    }

    @Test
    @DisplayName("Stage 2 (Lifetime ECL): 다년차 한계 PD 시퀀스에 대해 개별 현가 할인을 적용하여 합산한다.")
    void calculateEcl_Stage2_LifetimeEcl() {
        // 3년 만기: PD_1 = 0.02, PD_2 = 0.015, PD_3 = 0.01
        // LGD = 0.40, EAD = 50,000,000, Discount = 0.05
        // Year 1 = 0.02 * 0.40 * 50,000,000 / 1.05^1 = 400,000 / 1.05 = 380,952.38095
        // Year 2 = 0.015 * 0.40 * 50,000,000 / 1.05^2 = 300,000 / 1.1025 = 272,108.84353
        // Year 3 = 0.01 * 0.40 * 50,000,000 / 1.05^3 = 200,000 / 1.157625 = 172,767.51970
        // Total Lifetime = 380,952.38095 + 272,108.84353 + 172,767.51970 = 825,828.74418 -> 825828.7442
        List<BigDecimal> pds = Arrays.asList(
                new BigDecimal("0.02"),
                new BigDecimal("0.015"),
                new BigDecimal("0.01")
        );
        BigDecimal lgd = new BigDecimal("0.40");
        BigDecimal ead = new BigDecimal("50000000.0000");
        BigDecimal discountRate = new BigDecimal("0.05");

        BigDecimal result = calculator.calculateEcl(CrStaging.STAGE2, pds, lgd, ead, discountRate);

        assertThat(result).isEqualByComparingTo(new BigDecimal("825828.7442"));
    }

    @Test
    @DisplayName("입력 파라미터가 null이거나 비어있으면 0.0000을 안전하게 반환한다.")
    void calculateEcl_NullOrEmptyInputs() {
        BigDecimal result = calculator.calculateEcl(CrStaging.STAGE1, null, new BigDecimal("0.45"), new BigDecimal("10000"), new BigDecimal("0.05"));
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);

        result = calculator.calculateEcl(CrStaging.STAGE1, Collections.emptyList(), new BigDecimal("0.45"), new BigDecimal("10000"), new BigDecimal("0.05"));
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("다중 거시경제 시나리오(낙관/중립/비관) 가중치 합산 ECL을 정밀 산출한다.")
    void calculateWeightedEcl_MultiScenario() {
        BigDecimal optEcl = new BigDecimal("800000.0000");  // 낙관 20%
        BigDecimal baseEcl = new BigDecimal("1000000.0000"); // 중립 60%
        BigDecimal pessEcl = new BigDecimal("1500000.0000"); // 비관 20%

        BigDecimal result = calculator.calculateWeightedEcl(
                optEcl, baseEcl, pessEcl,
                new BigDecimal("0.20"), new BigDecimal("0.60"), new BigDecimal("0.20")
        );

        // Weighted = (800k * 0.2) + (1m * 0.6) + (1.5m * 0.2) = 160k + 600k + 300k = 1,060,000.0000
        assertThat(result).isEqualByComparingTo(new BigDecimal("1060000.0000"));
    }
}
