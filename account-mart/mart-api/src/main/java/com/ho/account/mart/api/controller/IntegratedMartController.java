package com.ho.account.mart.api.controller;

import com.ho.account.shared.finance.dto.ApiResponse;
import com.ho.account.shared.finance.entity.IntegratedRiskPosition;
import com.ho.account.mart.core.application.port.out.IntegratedRiskPositionRepository;
import com.ho.account.mart.core.application.port.out.OdsCustomerMstRepository;
import com.ho.account.mart.core.domain.ods.common.OdsCustomerMst;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * [API Controller] 통합 리스크 데이터 마트 제공 서비스
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/mart")
@RequiredArgsConstructor
public class IntegratedMartController {

    private final IntegratedRiskPositionRepository martRepository;
    private final OdsCustomerMstRepository customerRepository;

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<List<OdsCustomerMst>>> getAllCustomers() {
        return ResponseEntity.ok(ApiResponse.success(customerRepository.findAll()));
    }

    @GetMapping("/explorer")
    public ResponseEntity<ApiResponse<Page<IntegratedRiskPosition>>> getMartData(
            @RequestParam("baseDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success(martRepository.findByBaseDt(
                baseDate,
                PageRequest.of(page, size, Sort.by("accNo").ascending()))));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary(
            @RequestParam("baseDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
        return ResponseEntity.ok(ApiResponse.success(martRepository.getSummaryMetrics(baseDate)));
    }

    @GetMapping("/charts/staging")
    public ResponseEntity<ApiResponse<List<IntegratedRiskPositionRepository.StagingDistribution>>> getStagingData(
            @RequestParam("baseDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
        return ResponseEntity.ok(ApiResponse.success(martRepository.getStagingDistribution(baseDate)));
    }

    @GetMapping("/charts/sector")
    public ResponseEntity<ApiResponse<List<IntegratedRiskPositionRepository.SectorDistribution>>> getSectorData(
            @RequestParam("baseDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
        return ResponseEntity.ok(ApiResponse.success(martRepository.getSectorDistribution(baseDate)));
    }
}
