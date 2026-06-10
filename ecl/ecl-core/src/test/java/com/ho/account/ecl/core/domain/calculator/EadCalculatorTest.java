package com.ho.account.ecl.core.domain.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EadCalculatorTest {

    private EadCalculator eadCalculator;

    @BeforeEach
    void setUp() {
        eadCalculator = new EadCalculator();
    }

    @Test
    @DisplayName("✅ IFRS 9 모델 UCCR 10% CCF 및 미인출액 검증")
    void testCalculateEadWithUccr() {
        BigDecimal outstanding = new BigDecimal("10000000"); // 1,000만
        BigDecimal limit = new BigDecimal("50000000");       // 5,000만
        BigDecimal ccf = new BigDecimal("0.10");             // UCCR 10%

        EadCalculationResult result = eadCalculator.calculateAdvancedEAD(
                outstanding, limit, ccf, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("0.20"), new BigDecimal("0.45"));

        // EAD = 10,000,000 + (40,000,000 * 0.1) = 14,000,000
        assertEquals(0, new BigDecimal("14000000").compareTo(result.eadRaw()));
        assertEquals(0, new BigDecimal("14000000").compareTo(result.eadStar()));
    }

    @Test
    @DisplayName("✅ 부동산 담보 헤어컷 적용 CRM 공제 검증")
    void testCalculateEadStarWithCollateral() {
        BigDecimal outstanding = new BigDecimal("10000000"); 
        BigDecimal limit = new BigDecimal("20000000");        
        BigDecimal ccf = new BigDecimal("0.50");
        BigDecimal collateral = new BigDecimal("10000000");   
        BigDecimal baseHaircut = new BigDecimal("0.30");      

        EadCalculationResult result = eadCalculator.calculateAdvancedEAD(
                outstanding, limit, ccf, collateral, baseHaircut,
                new BigDecimal("0.20"), new BigDecimal("0.45"));

        // Raw EAD = 10M + (10M * 0.5) = 15M
        // Deduction = 10M * (1 - 0.3) = 7M
        // EAD* = 15M - 7M = 8M
        assertEquals(0, new BigDecimal("15000000").compareTo(result.eadRaw()));
        assertEquals(0, new BigDecimal("7000000").compareTo(result.crmDeduction()));
        assertEquals(0, new BigDecimal("8000000").compareTo(result.eadStar()));
    }

    @Test
    @DisplayName("모델 비율이 누락되면 임의 기본값으로 계산하지 않고 실패한다")
    void failClosedWhenModelRateIsMissing() {
        assertThrows(IllegalArgumentException.class, () -> eadCalculator.calculateAdvancedEAD(
                new BigDecimal("100"), new BigDecimal("120"), new BigDecimal("0.50"),
                BigDecimal.ZERO, BigDecimal.ZERO, null, new BigDecimal("0.45")));
    }
}
