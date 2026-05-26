package com.risk.credit.core.domain.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EadCalculatorTest {

    private EadCalculator eadCalculator;

    @BeforeEach
    void setUp() {
        eadCalculator = new EadCalculator();
    }

    @Test
    @DisplayName("✅ 바젤 IV UCCR 10% CCF 및 미인출액 검증")
    void testCalculateEadWithUccr() {
        BigDecimal outstanding = new BigDecimal("10000000"); // 1,000만
        BigDecimal limit = new BigDecimal("50000000");       // 5,000만
        BigDecimal ccf = new BigDecimal("0.10");             // UCCR 10%

        // public Object[] calculateAdvancedEAD(BigDecimal outstandingAmt, BigDecimal notionalAmt, BigDecimal ccfRate, BigDecimal collateralAmt, BigDecimal totalHaircut)
        // returns [eadStar, ccfRate, ead, crmDeduction, weightedLgd]
        Object[] result = eadCalculator.calculateAdvancedEAD(outstanding, limit, ccf, BigDecimal.ZERO, BigDecimal.ZERO);
        
        BigDecimal eadStar = (BigDecimal) result[0];
        BigDecimal eadRaw = (BigDecimal) result[2];

        // EAD = 10,000,000 + (40,000,000 * 0.1) = 14,000,000
        assertEquals(0, new BigDecimal("14000000").compareTo(eadRaw));
        assertEquals(0, new BigDecimal("14000000").compareTo(eadStar));
    }

    @Test
    @DisplayName("✅ 부동산 담보 헤어컷 적용 CRM 공제 검증")
    void testCalculateEadStarWithCollateral() {
        BigDecimal outstanding = new BigDecimal("10000000"); 
        BigDecimal limit = new BigDecimal("20000000");        
        BigDecimal ccf = new BigDecimal("0.50");
        BigDecimal collateral = new BigDecimal("10000000");   
        BigDecimal baseHaircut = new BigDecimal("0.30");      

        Object[] result = eadCalculator.calculateAdvancedEAD(outstanding, limit, ccf, collateral, baseHaircut);
        
        BigDecimal eadStar = (BigDecimal) result[0];
        BigDecimal eadRaw = (BigDecimal) result[2];
        BigDecimal crmDeduction = (BigDecimal) result[3];

        // Raw EAD = 10M + (10M * 0.5) = 15M
        // Deduction = 10M * (1 - 0.3) = 7M
        // EAD* = 15M - 7M = 8M
        assertEquals(0, new BigDecimal("15000000").compareTo(eadRaw));
        assertEquals(0, new BigDecimal("7000000").compareTo(crmDeduction));
        assertEquals(0, new BigDecimal("8000000").compareTo(eadStar));
    }
}