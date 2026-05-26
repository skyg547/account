package com.risk.credit.api.controller;

import com.risk.credit.core.application.service.stresstest.StressTestService;
import com.risk.credit.core.domain.result.CrSimulationResult;
import com.risk.common.dto.ApiResponse;
import com.risk.common.enums.StressScenario;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * [API] 신용리스크 스트레스 테스트 컨트롤러.
 * 경제 위기 시나리오를 가동하여 자본 적정성에 미치는 영향을 시뮬레이션합니다.
 *
 * 💡 [초보자를 위한 개념 설명]
 * 스트레스 테스트란 "만약 내일 당장 경제 위기가 온다면 우리 은행이 버틸 수 있을까?"를 미리 시험해 보는 것입니다.
 * 부도율(PD)이 2배로 치솟거나 담보 가치가 폭락하는 시나리오를 설정하여,
 * 평소보다 얼마나 많은 손실이 발생하고 자본이 깎이는지 미리 예측해보고 대비책을 세울 때 사용합니다.
 */
@RestController
@RequestMapping("/api/v1/credit-risk/stress-test")
@RequiredArgsConstructor

public class StressTestController {

    private final StressTestService stressTestService;

    /**
     * 특정 기준일 및 시나리오에 대한 스트레스 테스트 시뮬레이션을 실행한다.
     */
    @PostMapping("/run")
    public ApiResponse<String> runSimulation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate,
            @RequestParam StressScenario scenario) {

        stressTestService.runSimulation(baseDate, scenario);
        return ApiResponse.success("Simulation for " + scenario + " completed successfully.");
    }

    /**
     * 스트레스 테스트 시뮬레이션 상세 결과를 조회한다.
     */
    @GetMapping("/results")
    public ApiResponse<List<CrSimulationResult>> getResults(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate,
            @RequestParam StressScenario scenario) {

        return ApiResponse.success(stressTestService.getSimulationResults(baseDate, scenario));
    }

    /**
     * 스트레스 테스트 결과 요약 정보를 조회한다.
     */
    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate,
            @RequestParam StressScenario scenario) {

        return ApiResponse.success(stressTestService.getSimulationSummary(baseDate, scenario));
    }
}
