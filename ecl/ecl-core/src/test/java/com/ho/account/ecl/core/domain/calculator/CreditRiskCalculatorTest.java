package com.ho.account.ecl.core.domain.calculator;

import com.ho.account.shared.finance.enums.CrStaging;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [QA] 대손충당금(IFRS9) 계산기 (CreditRiskCalculator) 수학적 검증 테스트
 */
class CreditRiskCalculatorTest {

    private CreditRiskCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CreditRiskCalculator();
    }

    @Test
    @DisplayName("✅ IFRS 9 기대 신용 손실(ECL) 검증 - Stage 1 (12개월 PD)")
    void testCalculateEclStage1() {
        BigDecimal ead = new BigDecimal("1000000");
        BigDecimal lgd = new BigDecimal("0.45");
        List<BigDecimal> marginalPds = Arrays.asList(new BigDecimal("0.01"));
        BigDecimal discountRate = new BigDecimal("0.05");

        BigDecimal ecl = calculator.calculateEcl(CrStaging.STAGE1, marginalPds, lgd, ead, discountRate);

        // Expected: 4285.7143
        assertEquals(0, new BigDecimal("4285.7143").compareTo(ecl));
    }

    @Test
    @DisplayName("✅ IFRS 9 기대 신용 손실(ECL) 검증 - Stage 2 (Lifetime)")
    void testCalculateEclStage2() {
        BigDecimal ead = new BigDecimal("1000000");
        BigDecimal lgd = new BigDecimal("0.45");
        List<BigDecimal> marginalPds = Arrays.asList(new BigDecimal("0.02"), new BigDecimal("0.03"));
        BigDecimal discountRate = new BigDecimal("0.05");

        BigDecimal ecl = calculator.calculateEcl(CrStaging.STAGE2, marginalPds, lgd, ead, discountRate);

        assertTrue(ecl.compareTo(new BigDecimal("20000")) > 0);
    }

    @Test
    @DisplayName("✅ 국제 금융 규제 표준방법(SA) RWA 산출 검증")
    void testCalculateRwaSa() {
        BigDecimal ead = new BigDecimal("1000000");
        BigDecimal standardRw = new BigDecimal("0.75");
        BigDecimal rwa = calculator.calculateRwaSa(ead, standardRw);

        assertEquals(0, new BigDecimal("750000.0000").compareTo(rwa));
    }

    @Test
    @DisplayName("✅ 국제 금융 규제 내부등급법(IRB) RWA 산출 검증")
    void testCalculateRwaIrb() {
        BigDecimal pd = new BigDecimal("0.01");
        BigDecimal lgd = new BigDecimal("0.45");
        BigDecimal ead = new BigDecimal("1000000");
        double maturity = 2.5;

        CreditRiskCalculator.IrbResult result = calculator.calculateRwaIrb(pd, lgd, ead, maturity);

        assertNotNull(result.getRwa());
        assertNotNull(result.getKValue());
        assertNotNull(result.getRValue());

        assertTrue(result.getRwa().compareTo(new BigDecimal("45000")) > 0);
    }

    @Test
    @DisplayName("✅ 최소 부도율(PD Floor) 적용 검증")
    void testPdFloor() {
        BigDecimal lowPd = new BigDecimal("0.0001");
        BigDecimal floorPd = new BigDecimal("0.0005");
        BigDecimal lgd = new BigDecimal("0.45");
        BigDecimal ead = new BigDecimal("1000000");

        CreditRiskCalculator.IrbResult resultLow = calculator.calculateRwaIrb(lowPd, lgd, ead, 1.0);
        CreditRiskCalculator.IrbResult resultFloor = calculator.calculateRwaIrb(floorPd, lgd, ead, 1.0);

        assertEquals(resultLow.getRwa(), resultFloor.getRwa());
    }
}