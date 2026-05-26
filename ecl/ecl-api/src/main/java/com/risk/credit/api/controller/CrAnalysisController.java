package com.risk.credit.api.controller;

import com.risk.credit.core.application.service.monitoring.ConcentrationRiskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * [API] 신용 리스크 분석 및 통계 컨트롤러
 * 산업별 집중도 지수(HHI) 등을 통해 포트폴리오의 편중 상태를 조회합니다.
 *
 * 💡 [초보자를 위한 개념 설명]
 * HHI (Herfindahl-Hirschman Index)란 시장이나 포트폴리오가 얼마나 특정 쪽으로 쏠려 있는지 나타내는 지표입니다.
 * 예를 들어 은행 대출이 특정 산업(예: 건설업)에만 너무 많이 몰려 있다면,
 * 해당 산업이 어려워질 때 은행 전체가 위험해질 수 있습니다.
 * 이를 수치로 측정하여 '분산 투자'가 잘 되고 있는지 확인하는 지표입니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/credit-risk/analysis")
@RequiredArgsConstructor

public class CrAnalysisController {

    private final ConcentrationRiskService concentrationRiskService;

    /**
     * 산업 및 차주별 집중도(HHI) 분석 결과를 반환한다.
     */
    @GetMapping("/concentration")
    public Map<String, Object> getConcentrationRisk() {
        log.info("🔎 [집중도 분석] HHI 분석 요청을 처리합니다.");

        BigDecimal industryHhi = concentrationRiskService.calculateIndustryHhi();
        BigDecimal counterpartyHhi = concentrationRiskService.calculateCounterpartyHhi();

        Map<String, Object> response = new HashMap<>();
        response.put("industryHhi", industryHhi);
        response.put("counterpartyHhi", counterpartyHhi);

        // 집중도 수준 판정 (HHI 기준: 1,500 미만 분산, 1,500~2,500 보통, 2,500 초과 고집중)
        response.put("industryStatus", evaluateHhi(industryHhi));
        response.put("counterpartyStatus", evaluateHhi(counterpartyHhi));

        return response;
    }

    /**
     * HHI 수치에 따른 집중도 상태를 판정합니다.
     */
    private String evaluateHhi(BigDecimal hhi) {
        if (hhi.compareTo(new BigDecimal("1500")) < 0)
            return "낮음 (분산)";
        if (hhi.compareTo(new BigDecimal("2500")) <= 0)
            return "중간 (주의)";
        return "높음 (집중 위험)";
    }
}
