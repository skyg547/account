package com.risk.mart.api.marketdata.controller;

import com.risk.common.dto.ApiResponse;
import com.risk.mart.api.marketdata.dto.MarketRateRequest;
import com.risk.mart.api.marketdata.dto.MarketRateResponse;
import com.risk.mart.core.domain.marketdata.entity.MarketRate;
import com.risk.mart.core.domain.marketdata.service.MarketRateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 🏦 [API] 시장 금리(Market Rate) 관리 컨트롤러.
 * 국고채, CD금리, LIBOR 등 리스크 산출의 기초가 되는 다양한 시장 지표 금리를 관리합니다.
 *
 * 💡 [초보자를 위한 개념 설명]
 * 이 컨트롤러는 은행 외부의 '시장 상황'을 리스크 시스템에 알려주는 창구입니다.
 * "오늘 우리나라 3년 만기 국고채 금리는 몇 %인가 "와 같은 정보를 조회하거나 등록합니다.
 * 이 정보가 있어야 고객의 대출 금리가 적정한지, 혹은 시장 금리가 1% 올랐을 때
 * 우리 은행이 입게 될 손실이 얼마인지 정확히 계산할 수 있습니다.
 */
@RestController
@RequestMapping("/api/v1/market-data/market-rates")
@RequiredArgsConstructor
public class MarketRateController {

    private final MarketRateService marketRateService;

    /**
     * 신규 시장 금리 정보 단건 생성.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MarketRateResponse>> create(@Valid @RequestBody MarketRateRequest request) {
        MarketRate saved = marketRateService.saveRate(toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(toResponse(saved)));
    }

    /**
     * 시장 금리 대량(Batch) 등록.
     */
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<MarketRateResponse>>> createBatch(
            @Valid @RequestBody List<MarketRateRequest> requests) {
        List<MarketRate> saved = marketRateService
                .saveAllRates(requests.stream().map(this::toEntity).collect(Collectors.toList()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(saved.stream().map(this::toResponse).collect(Collectors.toList())));
    }

    /**
     * 특정 기준일의 모든 시장 금리 내역 조회.
     */
    @GetMapping("/by-date")
    public ResponseEntity<ApiResponse<List<MarketRateResponse>>> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
        return ResponseEntity.ok(ApiResponse.success(marketRateService.getRatesByDate(baseDate).stream()
                .map(this::toResponse)
                .collect(Collectors.toList())));
    }

    /**
     * 특정 기준일 및 금리 유형(예: KTB, CD)별 금리 조회.
     */
    @GetMapping("/by-date-type")
    public ResponseEntity<ApiResponse<List<MarketRateResponse>>> getByDateAndType(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate,
            @RequestParam String rateType) {
        return ResponseEntity.ok(ApiResponse.success(marketRateService.getRatesByType(baseDate, rateType).stream()
                .map(this::toResponse)
                .collect(Collectors.toList())));
    }

    /**
     * 특정 지표 금리의 과거 이력(Historical Data) 조회.
     */
    @GetMapping("/historical")
    public ResponseEntity<ApiResponse<List<MarketRateResponse>>> getHistorical(
            @RequestParam String rateName,
            @RequestParam Integer tenor,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity
                .ok(ApiResponse.success(marketRateService.getRateHistory(rateName, tenor, startDate, endDate).stream()
                        .map(this::toResponse)
                        .collect(Collectors.toList())));
    }

    /**
     * 현재 시스템에서 관리 중인 전체 금리 지표 명칭 리스트 조회.
     */
    @GetMapping("/names")
    public ResponseEntity<ApiResponse<List<String>>> getActiveRateNames() {
        return ResponseEntity.ok(ApiResponse.success(marketRateService.getActiveRateNames()));
    }

    private MarketRate toEntity(MarketRateRequest request) {
        return MarketRate.builder()
                .baseDate(request.getBaseDate())
                .rateName(request.getRateName())
                .rateType(request.getRateType())
                .currency(request.getCurrency())
                .tenorMonths(request.getTenor())
                .tenorLabel(request.getTenorLabel())
                .rate(request.getRate())
                .changeBp(request.getChangeBp())
                .source(request.getSource())
                .build();
    }

    private MarketRateResponse toResponse(MarketRate rate) {
        return MarketRateResponse.builder()
                .id(rate.getId())
                .baseDate(rate.getBaseDate())
                .rateName(rate.getRateName())
                .rateType(rate.getRateType())
                .currency(rate.getCurrency())
                .tenor(rate.getTenorMonths())
                .tenorLabel(rate.getTenorLabel())
                .rate(rate.getRate())
                .changeBp(rate.getChangeBp())
                .source(rate.getSource())
                .isActive(rate.getIsActive())
                .build();
    }
}