package com.ho.account.ecl.core.application.service.calculation;

import com.ho.account.shared.finance.enums.CrStaging;
import com.ho.account.ecl.core.application.port.out.CrMacroScenarioRepository;
import com.ho.account.ecl.core.domain.calculator.IfrsEclCalculator;
import com.ho.account.ecl.core.domain.model.CrMacroScenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForwardLookingEclServiceTest {

    @Mock private CrMacroScenarioRepository macroScenarioRepository;
    @Mock private IfrsEclCalculator eclCalculator;

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
        when(eclCalculator.calculateEcl(any(), anyList(), any(), any(), any()))
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

    @ParameterizedTest(name = "{0} scenario weights are rejected before calculating ECL, cached={4}")
    @MethodSource("invalidWeights")
    void shouldRejectInvalidScenarioWeightsBeforeCalculation(
            String caseName, BigDecimal boomWeight, BigDecimal baseWeight,
            BigDecimal recessionWeight, boolean cached) {
        List<CrMacroScenario> scenarios = List.of(
                scenario("BOOM", boomWeight),
                scenario("BASE", baseWeight),
                scenario("RECESSION", recessionWeight));
        if (cached) {
            when(macroScenarioRepository.findAll()).thenReturn(scenarios);
            flEclService.refreshCache();
        } else {
            when(macroScenarioRepository.findByApplyYear(2026)).thenReturn(scenarios);
        }

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> flEclService.calculateWeightedEcl(
                        CrStaging.STAGE1, List.of(new BigDecimal("0.01")),
                        new BigDecimal("0.45"), new BigDecimal("1000"),
                        new BigDecimal("0.05"), 2026));

        assertTrue(error.getMessage().contains("모델 입력 오류"));
        assertTrue(error.getMessage().contains("probabilityWeight"));
        verifyNoInteractions(eclCalculator);
    }

    private static Stream<Arguments> invalidWeights() {
        return Stream.<String[]>of(
                new String[] {"total 0.90", "0.20", "0.50", "0.20"},
                new String[] {"total 1.10", "0.20", "0.50", "0.40"},
                new String[] {"negative weight", "-0.10", "0.80", "0.30"},
                new String[] {"weight above one", "1.10", "0.00", "0.00"},
                new String[] {"null weight", null, "0.50", "0.50"})
                .flatMap(weights -> Stream.of(
                        Arguments.of(weights[0], decimal(weights[1]), decimal(weights[2]), decimal(weights[3]), false),
                        Arguments.of(weights[0], decimal(weights[1]), decimal(weights[2]), decimal(weights[3]), true)));
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static CrMacroScenario scenario(String type, BigDecimal weight) {
        return CrMacroScenario.builder()
                .applyYear(2026)
                .scenarioType(type)
                .probabilityWeight(weight)
                .pdAdjustmentFactor(BigDecimal.ONE)
                .build();
    }

    @Test
    @DisplayName("시나리오가 0건이면 단일 시나리오 ECL을 유지한다")
    void shouldUseSingleScenarioFallbackWhenNoScenariosExist() {
        when(macroScenarioRepository.findByApplyYear(2026)).thenReturn(List.of());
        when(eclCalculator.calculateEcl(any(), anyList(), any(), any(), any()))
                .thenReturn(new BigDecimal("100.0000"));

        ForwardLookingEclService.FlEclResult result = flEclService.calculateWeightedEcl(
                CrStaging.STAGE1, List.of(new BigDecimal("0.01")),
                new BigDecimal("0.45"), new BigDecimal("1000"),
                new BigDecimal("0.05"), 2026);

        assertEquals(0, new BigDecimal("100.0000").compareTo(result.getWeightedEcl()));
        assertEquals(0, result.getWeightedEcl().compareTo(result.getEclBase()));
        assertEquals(0, result.getWeightedEcl().compareTo(result.getEclBoom()));
        assertEquals(0, result.getWeightedEcl().compareTo(result.getEclRecession()));
    }
}
