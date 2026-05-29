package com.ho.account.ecl.api.controller;

import com.ho.account.ecl.core.application.service.allowance.AllowanceCalculationService;
import com.ho.account.ecl.core.domain.result.AllowanceEclResult;
import com.ho.account.shared.finance.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
/**
 * [API] IFRS 9 대손충당금 산출 결과 및 요약 컨트롤러.
 */
@RestController
@RequestMapping("/api/v1/ifrs/allowance/results")
@RequiredArgsConstructor
public class AllowanceResultController {

    private final AllowanceCalculationService allowanceCalculationService;

    /**
     * 특정 기준일의 IFRS 9 대손충당금 산출 요약 지표를 조회한다.
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Object>> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
        return ResponseEntity.ok(ApiResponse.success(allowanceCalculationService.getAllowanceSummary(baseDate)));
    }

    /**
     * 단일 계좌에 대한 대손충당금(IFRS9) 산출을 수행한다.
     */
    @PostMapping("/calculate/{accountId}")
    public ResponseEntity<ApiResponse<AllowanceEclResult>> calculateAllowance(
            @PathVariable @NonNull Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @NonNull LocalDate baseDate) {
        return ResponseEntity.ok(ApiResponse.success(
                allowanceCalculationService.calculateAccountAllowance(accountId, baseDate)));
    }
}

