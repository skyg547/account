package com.ho.account.ecl.core.domain.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LgdCalculatorTest {

    private LgdCalculator lgdCalculator;

    @BeforeEach
    void setUp() {
        lgdCalculator = new LgdCalculator();
    }

    @Test
    @DisplayName("담보부 익스포저(10M)와 무담보 익스포저(40M)가 섞여있을 때 가중 LGD를 산출한다.")
    void calculateWeightedLgd_MixedSecuredAndUnsecured() {
        BigDecimal securedAmt = new BigDecimal("10000000.0000");   // Secured LGD = 10%
        BigDecimal unsecuredAmt = new BigDecimal("40000000.0000"); // Unsecured LGD = 45%
        BigDecimal totalEad = new BigDecimal("50000000.0000");

        BigDecimal securedLgd = new BigDecimal("0.10");
        BigDecimal unsecuredLgd = new BigDecimal("0.45");

        // Weighted = (10M * 0.10 + 40M * 0.45) / 50M = (1M + 18M) / 50M = 19M / 50M = 0.38 (38%)
        BigDecimal result = lgdCalculator.calculateWeightedLgd(
                securedAmt, unsecuredAmt, totalEad,
                securedLgd, unsecuredLgd, BigDecimal.ONE, new BigDecimal("0.05")
        );

        assertThat(result).isEqualByComparingTo(new BigDecimal("0.380000"));
    }

    @Test
    @DisplayName("경기 침체기 보정 계수(Downturn Factor = 1.20) 적용 시 LGD가 20% 할증된다.")
    void calculateWeightedLgd_DownturnAdjustment() {
        BigDecimal securedAmt = BigDecimal.ZERO;
        BigDecimal unsecuredAmt = new BigDecimal("10000000.0000");
        BigDecimal totalEad = new BigDecimal("10000000.0000");

        BigDecimal securedLgd = new BigDecimal("0.10");
        BigDecimal unsecuredLgd = new BigDecimal("0.45");
        BigDecimal downturnFactor = new BigDecimal("1.20"); // 20% 침체기 할증

        // Raw = 0.45, Downturn = 0.45 * 1.20 = 0.540000 (54%)
        BigDecimal result = lgdCalculator.calculateWeightedLgd(
                securedAmt, unsecuredAmt, totalEad,
                securedLgd, unsecuredLgd, downturnFactor, new BigDecimal("0.05")
        );

        assertThat(result).isEqualByComparingTo(new BigDecimal("0.540000"));
    }

    @Test
    @DisplayName("완전 담보부 자산이라도 LGD Floor(0.05) 하한선이 적용된다.")
    void calculateWeightedLgd_LgdFloorApplied() {
        BigDecimal securedAmt = new BigDecimal("10000000.0000");
        BigDecimal unsecuredAmt = BigDecimal.ZERO;
        BigDecimal totalEad = new BigDecimal("10000000.0000");

        BigDecimal securedLgd = new BigDecimal("0.02"); // Raw = 2% < Floor(5%)
        BigDecimal unsecuredLgd = new BigDecimal("0.45");
        BigDecimal lgdFloor = new BigDecimal("0.05");

        BigDecimal result = lgdCalculator.calculateWeightedLgd(
                securedAmt, unsecuredAmt, totalEad,
                securedLgd, unsecuredLgd, BigDecimal.ONE, lgdFloor
        );

        assertThat(result).isEqualByComparingTo(new BigDecimal("0.050000"));
    }
}
