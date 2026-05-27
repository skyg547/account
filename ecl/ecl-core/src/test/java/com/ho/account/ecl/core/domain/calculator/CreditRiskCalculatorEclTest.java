package com.ho.account.ecl.core.domain.calculator;

import com.ho.account.shared.finance.enums.CrStaging;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreditRiskCalculatorEclTest {

    private CreditRiskCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CreditRiskCalculator();
    }

    @Test
    @DisplayName("✅ Stage 1 ECL 산출 검증")
    void testCalculateStage1Ecl() {
        BigDecimal ead = new BigDecimal("1000000");
        BigDecimal lgd = new BigDecimal("0.45");
        List<BigDecimal> marginalPds = Arrays.asList(new BigDecimal("0.02"), new BigDecimal("0.03"));
        BigDecimal effectiveInterestRate = new BigDecimal("0.05");

        // calculateEcl(CrStaging stage, List<BigDecimal> marginalPds, BigDecimal lgd, BigDecimal ead, BigDecimal effectiveInterestRate)
        BigDecimal ecl = calculator.calculateEcl(CrStaging.STAGE1, marginalPds, lgd, ead, effectiveInterestRate);

        // Expected: 0.02 * 0.45 * 1,000,000 / 1.05 = 8571.4286
        assertEquals(0, new BigDecimal("8571.4286").compareTo(ecl.setScale(4, RoundingMode.HALF_UP)));
    }

    @Test
    @DisplayName("✅ Stage 2 ECL 산출 검증 (Lifetime)")
    void testCalculateStage2Ecl() {
        BigDecimal ead = new BigDecimal("1000000");
        BigDecimal lgd = new BigDecimal("0.45");
        List<BigDecimal> marginalPds = Arrays.asList(new BigDecimal("0.02"), new BigDecimal("0.03"));
        BigDecimal effectiveInterestRate = new BigDecimal("0.05");

        BigDecimal ecl = calculator.calculateEcl(CrStaging.STAGE2, marginalPds, lgd, ead, effectiveInterestRate);

        // Expected: 8571.4286 + (0.03 * 0.45 * 1,000,000 / 1.05^2) = 20816.3265
        assertEquals(0, new BigDecimal("20816.3265").compareTo(ecl.setScale(4, RoundingMode.HALF_UP)));
    }
}