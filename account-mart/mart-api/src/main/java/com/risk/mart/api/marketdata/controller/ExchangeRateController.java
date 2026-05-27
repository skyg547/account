package com.risk.mart.api.marketdata.controller;

import com.risk.common.dto.ApiResponse;
import com.risk.common.enums.CurrencyCode;
import com.risk.mart.api.marketdata.dto.ExchangeRateRequest;
import com.risk.mart.api.marketdata.dto.ExchangeRateResponse;
import com.risk.mart.core.domain.marketdata.ExchangeRate;
import com.risk.mart.core.domain.marketdata.service.ExchangeRateService;
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
 * 🏦 [API] 환율(Exchange Rate) 관리 컨트롤러.
 * 시장에서 수집된 통화별 환율 정보를 조회하고 수동 등록 기능을 제공합니다.
 *
 * 💡 [초보자를 위한 개념 설명]
 * 이 컨트롤러는 리스크 시스템의 '환전소'와 같은 역할을 합니다. 
 * 외화로 빌려준 돈을 한국 돈으로 얼마인지 계산할 때 필요한 환율 데이터를 조회합니다. 
 * 매일의 기준 환율뿐만 아니라 특정 기간 동안 환율이 어떻게 변했는지(이력 조회)도 확인할 수 있어 
 * 환율 변동에 따른 리스크 분석의 기초를 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/market-data/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    /**
     * 신규 환율 정보 단건 생성.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ExchangeRateResponse>> create(@Valid @RequestBody ExchangeRateRequest request) {
        ExchangeRate saved = exchangeRateService.saveRate(ExchangeRate.builder()
                .baseDate(request.getBaseDate())
                .baseCurrency(request.getBaseCurrency())
                .quoteCurrency(request.getQuoteCurrency())
                .baseRate(request.getRate())
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(toResponse(saved)));
    }

    /**
     * 특정 기준일의 모든 환율 정보 조회.
     */
    @GetMapping("/by-date")
    public ResponseEntity<ApiResponse<List<ExchangeRateResponse>>> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
        return ResponseEntity.ok(ApiResponse.success(exchangeRateService.getRatesByDate(baseDate).stream()
                .map(this::toResponse)
                .collect(Collectors.toList())));
    }

    /**
     * 특정 통화 쌍(Pair) 및 기준일의 환율 조회.
     */
    @GetMapping("/pair")
    public ResponseEntity<ApiResponse<ExchangeRateResponse>> getPair(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate,
            @RequestParam CurrencyCode baseCurrency,
            @RequestParam CurrencyCode quoteCurrency) {
        return ResponseEntity.ok(ApiResponse.success(exchangeRateService.getRate(baseDate, baseCurrency, quoteCurrency)
                .map(this::toResponse)
                .orElseThrow()));
    }

    /**
     * 시스템에 저장된 가장 최신 환율 정보 조회.
     */
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<ExchangeRateResponse>> getLatest(
            @RequestParam CurrencyCode baseCurrency,
            @RequestParam CurrencyCode quoteCurrency) {
        return ResponseEntity.ok(ApiResponse.success(exchangeRateService.getLatestRate(baseCurrency, quoteCurrency)
                .map(this::toResponse)
                .orElseThrow()));
    }

    /**
     * 특정 기간 동안의 환율 이력(Historical Data) 조회.
     */
    @GetMapping("/historical")
    public ResponseEntity<ApiResponse<List<ExchangeRateResponse>>> getHistorical(
            @RequestParam CurrencyCode baseCurrency,
            @RequestParam CurrencyCode quoteCurrency,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(exchangeRateService.getHistorical(baseCurrency, quoteCurrency, startDate, endDate).stream()
                .map(this::toResponse)
                .collect(Collectors.toList())));
    }

    private ExchangeRateResponse toResponse(ExchangeRate rate) {
        return ExchangeRateResponse.builder()
                .id(rate.getId())
                .baseDate(rate.getBaseDate())
                .baseCurrency(rate.getBaseCurrency())
                .quoteCurrency(rate.getQuoteCurrency())
                .rate(rate.getBaseRate())
                .build();
    }
}
