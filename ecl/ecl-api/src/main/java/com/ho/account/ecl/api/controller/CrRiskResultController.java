package com.ho.account.ecl.api.controller;

import com.ho.account.ecl.core.application.service.calculation.CreditRiskService;
import com.ho.account.ecl.core.domain.result.CrRiskResult;
import com.ho.account.shared.finance.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * [API] 대손충당금(IFRS9) 최종 결과 및 통계 컨트롤러.
 * 산출이 완료된 기대손실(ECL), 위험가중자산(RWA) 및 각종 대손충당금 지표를 조회합니다.
 *
 * 💡 [초보자를 위한 개념 설명]
 * 모든 계산이 끝나면 그 결과가 '리스크 결과 테이블(CrRiskResult)'에 저장됩니다.
 * 이 컨트롤러는 "우리 은행이 2024년 12월 말 기준으로 대출 자산에 대해 총 얼마의 대손충당금을 쌓아야 하는가?"
 * 또는 "규제 자본 비율은 안정적인가?" 하는 핵심 데이터를 사용자에게 보여주는 창구입니다.
 */
@RestController
@RequestMapping("/api/v1/credit-risk/results")
@RequiredArgsConstructor
public class CrRiskResultController {

    private final CreditRiskService creditRiskService;
    private final com.ho.account.ecl.core.application.service.CrConcentrationService concentrationService;

    /**
     * 특정 기준일의 대손충당금(IFRS9) 산출 요약 지표를 조회한다.
     * 
     * 💡 [초보자를 위한 가이드]
     * 대시보드 상단에 들어갈 전사 합계 규모(총 RWA, 평균 PD 등)를 계산하여 한 눈에 보여줍니다.
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Object>> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
        // 실제 운영 시에는 DB 집계 결과를 반환함. 현재는 서비스에서 제공하는 요약 정보를 연동.
        return ResponseEntity.ok(ApiResponse.success(creditRiskService.getResultsSummary(baseDate)));
    }

    /**
     * 산업별 리스크 집중도 분석 결과를 조회한다.
     * 
     * 💡 [비즈니스 가이드] 
     * 특정 산업에 대출이 쏠려있지 않은지 확인하여 포트폴리오의 건전성을 관리합니다.
     */
    @GetMapping("/concentration")
    public ResponseEntity<ApiResponse<Object>> getConcentration(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
        return ResponseEntity.ok(ApiResponse.success(concentrationService.analyzeIndustryConcentration(baseDate.toString())));
    }

    /**
     * 특정 산업군의 상세 리스크 내역(차주별 분포)을 조회한다.
     * 
     * 💡 [비즈니스 가이드]
     * 집중도 차트에서 특정 산업을 클릭했을 때, 어떤 회사가 우리 은행의 돈을 
     * 가장 많이 빌리고 있는지 상세히 보여주기 위한 API입니다.
     */
    @GetMapping("/concentration/{industryCode}")
    public ResponseEntity<ApiResponse<Object>> getConcentrationDetail(
            @PathVariable String industryCode) {
        return ResponseEntity.ok(ApiResponse.success(concentrationService.getIndustryDetails(industryCode)));
    }

    /**
     * 단일 계좌에 대한 대손충당금(IFRS9) 산출을 수행한다.
     */
    @PostMapping("/calculate/{accountId}")
    public ResponseEntity<ApiResponse<CrRiskResult>> calculateRisk(
            @PathVariable @NonNull Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @NonNull LocalDate baseDate) {
        return ResponseEntity.ok(ApiResponse.success(
                creditRiskService.calculateAccountRisk(accountId, baseDate)));
    }
}
