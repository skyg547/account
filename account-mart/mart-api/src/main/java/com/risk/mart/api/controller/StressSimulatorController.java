package com.risk.mart.api.controller;

import com.risk.common.dto.ApiResponse;
import com.risk.common.enums.StressScenario;
import com.risk.mart.core.domain.mart.service.StressSimulatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * [시뮬레이션] 리스크 스트레스 테스트 시뮬레이터 컨트롤러
 *
 * 💡 [초보자를 위한 개념 설명]
 * 스트레스 테스트(Stress Test)란 "만약 경제 위기가 닥친다면 은행이 얼마나 견딜 수 있을까 "를 미리 시뮬레이션 해보는 것입니다.
 * 예를 들어 '부동산 가격 30% 폭락'이나 '금리 2% 급등' 같은 비정상적인 상황(Scenario)을 가정하고,
 * 이에 따른 자산 가치 하락과 자본 적정성 변화를 즉석에서 분석합니다.
 */
@RestController
@RequestMapping("/api/v1/mart/simulate")
@RequiredArgsConstructor
public class StressSimulatorController {

    private final StressSimulatorService simulatorService;

    /**
     * 특정 기준일자와 스트레스 시나리오를 바탕으로 영향도 분석 시뮬레이션을 실행한다.
     * 
     * @param baseDate 분석 기준일자
     * @param scenario 적용할 경제 위기 시나리오 (예: SEVERE_CRISIS, INTEREST_RATE_SHOCK 등)
     * @return 시나리오 적용 전/후의 RWA 및 자본 비율 변화 결과
     */
    @GetMapping("/stress")
    public ApiResponse<Map<String, Object>> simulateStress(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate,
            @RequestParam StressScenario scenario) {

        Map<String, Object> result = simulatorService.simulateStress(baseDate, scenario);
        return ApiResponse.success(result);
    }
}
