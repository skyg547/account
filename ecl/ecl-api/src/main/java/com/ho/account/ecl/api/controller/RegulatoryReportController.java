package com.ho.account.ecl.api.controller;

import com.ho.account.ecl.core.application.service.monitoring.RegulatoryReportService;
import com.ho.account.ecl.core.domain.result.RegulatoryReportSummary;
import com.ho.account.shared.finance.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * [API] 규제 및 경영 보고용 컨트롤러.
 * 경영진 보고용 지표 및 규제 당국 제출용 집계 대손충당금 지표를 제공합니다.
 *
 * 💡 [초보자를 위한 개념 설명]
 * 이 서비스는 개별 대출 건들의 대손충당금(IFRS9) 산출 결과를 한데 모아 '우리 은행의 전체적인 건강 상태'를 보여줍니다.
 * "은행 자본 대비 위험 자산은 얼마인가?", "금융감독원 기준에 맞게 공시가 가능한가?" 등의
 * 최종적인 보고용 데이터를 요약하여 보여주는 역할을 합니다.
 */
@RestController
@RequestMapping("/api/v1/credit-risk/report")
@RequiredArgsConstructor
public class RegulatoryReportController {

    private final RegulatoryReportService reportService;

    /**
     * 특정 기준일의 리스크 요약 보고서를 조회한다.
     */
    @GetMapping("/summary")
    public ApiResponse<RegulatoryReportSummary> getExecutiveSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {

        RegulatoryReportSummary summary = reportService.generateExecutiveSummary(baseDate);
        return ApiResponse.success(summary);
    }
}
