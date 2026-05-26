package com.risk.credit.core.application.service.calculation;

import com.risk.common.enums.CrStaging;
import com.risk.credit.core.application.port.out.CrMacroScenarioRepository;
import com.risk.credit.core.domain.calculator.CreditRiskCalculator;
import com.risk.credit.core.domain.model.CrMacroScenario;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * [Service] IFRS 9 미래전망(Forward-Looking) ECL 산출 서비스
 * 
 * 💡 [초보자를 위한 개념 설명]
 * IFRS 9은 "미래에 닥칠 수 있는 위험"을 미리 준비하라는 회계 기준입니다.
 * 단순히 지금 상황이 좋다고 안심하는 게 아니라, "나중에 경기가 나빠지면 얼마나 더 손해를 볼까?"를
 * 확률적으로 계산합니다. 호황, 평균, 침체 시나리오별로 예상되는 손실을 각각 구한 뒤,
 * 각 시나리오가 일어날 확률을 곱해 최종적인 '가중평균 충당금'을 결정합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ForwardLookingEclService {

    private final CrMacroScenarioRepository macroScenarioRepository;
    private final CreditRiskCalculator riskCalculator;

    /**
     * 거시경제 시나리오 캐시 (ApplyYear -> List of CrMacroScenario)
     * 💡 배치 시작 전 모든 시나리오 데이터를 일괄 로드하여 보관합니다.
     */
    private final ConcurrentHashMap<Integer, List<CrMacroScenario>> scenarioCache = new ConcurrentHashMap<>();

    /**
     * [고도화] 모든 거시경제 시나리오 데이터를 메모리에 캐싱합니다.
     */
    public void refreshCache() {
        log.info("📦 [미래전망] 거시경제 시나리오 캐시 갱신 중...");
        List<CrMacroScenario> allScenarios = macroScenarioRepository.findAll();

        scenarioCache.clear();
        scenarioCache.putAll(allScenarios.stream()
                .collect(Collectors.groupingBy(CrMacroScenario::getApplyYear)));

        log.info("✅ [미래전망] 총 {}개 연도의 시나리오 데이터가 캐싱되었습니다.", scenarioCache.size());
    }

    @Getter
    @Builder
    public static class FlEclResult {
        private BigDecimal weightedEcl;
        private BigDecimal eclBoom;
        private BigDecimal eclBase;
        private BigDecimal eclRecession;
    }

    /**
     * 시나리오 가중치를 반영한 미래전망 ECL을 산출한다.
     */
    public FlEclResult calculateWeightedEcl(
            CrStaging stage,
            List<BigDecimal> marginalPds,
            BigDecimal lgd,
            BigDecimal ead,
            BigDecimal discountRate,
            Integer applyYear) {

        // 1. 캐시에서 해당 연도의 시나리오 조회
        List<CrMacroScenario> scenarios = scenarioCache.get(applyYear);

        // 2. 캐시 미존재 시 Fallback (실시간 조회)
        if (scenarios == null || scenarios.isEmpty()) {
            log.debug("🔍 [미래전망] 캐시에 없는 연도({}) 시나리오 실시간 조회 시도", applyYear);
            scenarios = macroScenarioRepository.findByApplyYear(applyYear);
        }

        if (scenarios.isEmpty()) {
            log.warn("⚠️ [미래전망] {}년도 거시경제 시나리오가 없습니다. 단일 시나리오로 산출합니다.", applyYear);
            BigDecimal ecl = riskCalculator.calculateEcl(stage, marginalPds, lgd, ead, discountRate);
            return FlEclResult.builder()
                    .weightedEcl(ecl).eclBase(ecl).eclBoom(ecl).eclRecession(ecl).build();
        }

        BigDecimal totalWeightedEcl = BigDecimal.ZERO;
        BigDecimal eclBoom = BigDecimal.ZERO;
        BigDecimal eclBase = BigDecimal.ZERO;
        BigDecimal eclRecession = BigDecimal.ZERO;

        for (CrMacroScenario scenario : scenarios) {
            // 1. 시나리오별 PiT PD 조정 (TTC PD * Adjustment Factor)
            List<BigDecimal> adjustedPds = marginalPds.stream()
                    .map(pd -> pd.multiply(scenario.getPdAdjustmentFactor())
                            .min(new BigDecimal("1.0000"))) // 100% 상한
                    .toList();

            // 2. 시나리오별 ECL 산출
            BigDecimal scenarioEcl = riskCalculator.calculateEcl(stage, adjustedPds, lgd, ead, discountRate);

            // 3. 가중평균 합산
            totalWeightedEcl = totalWeightedEcl.add(scenarioEcl.multiply(scenario.getProbabilityWeight()));

            // 결과 기록용 매핑
            if ("BOOM".equalsIgnoreCase(scenario.getScenarioType())) eclBoom = scenarioEcl;
            else if ("BASE".equalsIgnoreCase(scenario.getScenarioType())) eclBase = scenarioEcl;
            else if ("RECESSION".equalsIgnoreCase(scenario.getScenarioType())) eclRecession = scenarioEcl;
        }

        return FlEclResult.builder()
                .weightedEcl(totalWeightedEcl.setScale(4, RoundingMode.HALF_UP))
                .eclBoom(eclBoom)
                .eclBase(eclBase)
                .eclRecession(eclRecession)
                .build();
    }
}
