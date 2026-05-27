package com.ho.account.ecl.core.application.service.calculation;

import com.ho.account.shared.finance.enums.CrStaging;
import com.ho.account.ecl.core.application.port.out.CrMacroScenarioRepository;
import com.ho.account.ecl.core.domain.calculator.CreditRiskCalculator;
import com.ho.account.ecl.core.domain.model.CrMacroScenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForwardLookingEclServiceTest {

    @Mock private CrMacroScenarioRepository macroScenarioRepository;
    @Mock private CreditRiskCalculator riskCalculator;

    @InjectMocks
    private ForwardLookingEclService flEclService;

    @Test
    @DisplayName("✅ 미래전망 시나리오 가중평균 ECL 산출 검증")
    void shouldCalculateWeightedAverageEcl() {
        // [Given]
        // 호황(0.2, 스칼라 0.8), 평균(0.5, 스칼라 1.0), 침체(0.3, 스칼라 1.5)
        CrMacroScenario boom = CrMacroScenario.builder().scenarioType("BOOM").probabilityWeight(new BigDecimal("0.2")).pdAdjustmentFactor(new BigDecimal("0.8")).build();
        CrMacroScenario base = CrMacroScenario.builder().scenarioType("BASE").probabilityWeight(new BigDecimal("0.5")).pdAdjustmentFactor(new BigDecimal("1.0")).build();
        CrMacroScenario recession = CrMacroScenario.builder().scenarioType("RECESSION").probabilityWeight(new BigDecimal("0.3")).pdAdjustmentFactor(new BigDecimal("1.5")).build();

        when(macroScenarioRepository.findByApplyYear(2026)).thenReturn(List.of(boom, base, recession));
        
        // Mock ECL values for each scenario
        when(riskCalculator.calculateEcl(any(), anyList(), any(), any(), any()))
            .thenReturn(new BigDecimal("80"))  // Boom
            .thenReturn(new BigDecimal("100")) // Base
            .thenReturn(new BigDecimal("150"));// Recession

        // [When]
        ForwardLookingEclService.FlEclResult result = flEclService.calculateWeightedEcl(
                CrStaging.STAGE1, List.of(new BigDecimal("0.01")), new BigDecimal("0.45"), new BigDecimal("1000"), new BigDecimal("0.05"), 2026);

        // [Then]
        // (80 * 0.2) + (100 * 0.5) + (150 * 0.3) = 16 + 50 + 45 = 111
        assertEquals(0, new BigDecimal("111").compareTo(result.getWeightedEcl()));
        assertEquals(0, new BigDecimal("80").compareTo(result.getEclBoom()));
        assertEquals(0, new BigDecimal("150").compareTo(result.getEclRecession()));
    }
}
