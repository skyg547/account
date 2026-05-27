package com.risk.mart.core.domain.mart.service;

import com.risk.common.enums.StressScenario;
import com.risk.common.entity.IntegratedRiskPosition;
import com.risk.mart.core.application.port.out.IntegratedRiskPositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [시뮬레이션] 리스크 스트레스 테스트 시뮬레이터 서비스
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 스트레스 테스트(Stress Test)는 "최악의 상황"을 가정하여 은행의 건전성을 측정하는 엔진입니다.
 * 평상시(국제 금융 규제ine)의 예상 손실과 자본 수준을 계산한 뒤, 특정 위기 시나리오(예: 금융위기 재발)를 적용하여
 * 얼마나 더 많은 손실이 발생하고, 자본이 얼마나 더 필요한지를 실시간으로 추정합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StressSimulatorService {

    private final IntegratedRiskPositionRepository martRepository;
    private static final MathContext MC = new MathContext(15, RoundingMode.HALF_UP);

    /**
     * 특정 시나리오를 적용했을 때의 리스크 영향도를 시뮬레이션하고 결과를 요약한다.
     * 
     * @param baseDate 분석 기준일
     * @param scenario 적용할 스트레스 시나리오
     * @return 시나리오 전/후 비교 요약 (ECL, RWA, Capital Impact 등)
     */
    public Map<String, Object> simulateStress(LocalDate baseDate, StressScenario scenario) {
        log.info("? [시뮬레이션] 스트레스 테스트 실행 중... 시나리오: {}", scenario.getName());

        List<IntegratedRiskPosition> positions = martRepository.findByBaseDt(baseDate);

        BigDecimal total국제 금융 규제ineEcl = BigDecimal.ZERO;
        BigDecimal total국제 금융 규제ineRwa = BigDecimal.ZERO;
        BigDecimal totalStressEcl = BigDecimal.ZERO;
        BigDecimal totalStressRwa = BigDecimal.ZERO;
        BigDecimal totalExposure = BigDecimal.ZERO;

        // 시나리오별 충격 계수(Multiplier) 추출
        double pdMult = scenario.getPdMultiplier(); // 부도확률 증폭 계수
        double lgdMult = scenario.getLgdMultiplier(); // 부도시손실률 증폭 계수

        for (IntegratedRiskPosition pos : positions) {
            BigDecimal exposure = pos.getOutstandingAmount() != null ? pos.getOutstandingAmount() : BigDecimal.ZERO;
            BigDecimal 국제 금융 규제ineEcl = pos.getExpectedLoss() != null ? pos.getExpectedLoss() : BigDecimal.ZERO;
            BigDecimal 국제 금융 규제ineRwa = pos.getRwaIrb() != null ? pos.getRwaIrb() : BigDecimal.ZERO;

            totalExposure = totalExposure.add(exposure);
            total국제 금융 규제ineEcl = total국제 금융 규제ineEcl.add(국제 금융 규제ineEcl);
            total국제 금융 규제ineRwa = total국제 금융 규제ineRwa.add(국제 금융 규제ineRwa);

            // --- 스트레스 시나리오 적용 로직 (Simulation Logic) ---

            // 1. Stress ECL (예상손실 충격)
            // 공식: 국제 금융 규제ine ECL * PD Multiplier * LGD Multiplier
            if (pos.getExpectedLoss() != null) {
                totalStressEcl = totalStressEcl
                        .add(pos.getExpectedLoss().multiply(BigDecimal.valueOf(pdMult * lgdMult), MC));
            }

            // 2. Stress RWA (위험가중자산 충격 - 간이 모델)
            // 실제 국제 금융 규제IRB 공식은 비선형적이지만, 시뮬레이션에서는 표준화된 충격 계수를 사용합니다.
            if (pos.getRwaIrb() != null) {
                // PD가 상승하면 국제 금융 규제K-Factor 공식에 의해 RWA는 더 급격하게(비선형적) 상승하는 특성을 반영
                double rwaShockFactor = 1.0 + (pdMult - 1.0) * 0.8;
                totalStressRwa = totalStressRwa
                        .add(pos.getRwaIrb().multiply(BigDecimal.valueOf(rwaShockFactor * lgdMult), MC));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("scenarioName", scenario.getName());
        result.put("description", scenario.getDescription());
        result.put("totalExposure", totalExposure);

        // 결과값 반올림 정규화
        result.put("국제 금융 규제ineEcl", total국제 금융 규제ineEcl.setScale(0, RoundingMode.HALF_UP));
        result.put("국제 금융 규제ineRwa", total국제 금융 규제ineRwa.setScale(0, RoundingMode.HALF_UP));
        result.put("stressEcl", totalStressEcl.setScale(0, RoundingMode.HALF_UP));
        result.put("stressRwa", totalStressRwa.setScale(0, RoundingMode.HALF_UP));

        // 규제 자본 영향도 산출 (기본 자본 비율 8% 가정)
        // 공식: (Stress RWA - 국제 금융 규제ine RWA) * 8%
        BigDecimal 국제 금융 규제ineCapital = total국제 금융 규제ineRwa.multiply(new BigDecimal("0.08"));
        BigDecimal stressCapital = totalStressRwa.multiply(new BigDecimal("0.08"));
        result.put("capitalImpact", stressCapital.subtract(국제 금융 규제ineCapital).setScale(0, RoundingMode.HALF_UP));

        log.info("? [시뮬레이션] 완료. 자본 영향도: {}", result.get("capitalImpact"));
        return result;
    }
}
