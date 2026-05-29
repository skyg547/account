package com.ho.account.mart.api.marketdata.controller;

import com.ho.account.shared.finance.dto.ApiResponse;
import com.ho.account.shared.finance.enums.CurrencyCode;
import com.ho.account.mart.api.marketdata.dto.YieldCurveRequest;
import com.ho.account.mart.api.marketdata.dto.YieldCurveResponse;
import com.ho.account.mart.core.domain.marketdata.YieldCurve;
import com.ho.account.mart.core.domain.marketdata.YieldCurvePoint;
import com.ho.account.mart.core.domain.marketdata.service.YieldCurveService;
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
 * 🏦 [API] 수익률 곡선(Yield Curve) 관리 컨트롤러.
 * 통화별, 만기별 금리 구조인 수익률 곡선을 관리하며 가치평가 및 대손충당금 민감도 분석의 기초 데이터를 제공합니다.
 *
 * 💡 [초보자를 위한 개념 설명]
 * 이 컨트롤러는 재무 결산 시스템의 '미래 금리 예측지도'를 관리합니다.
 * "오늘 빌린 돈의 금리와 10년 뒤에 갚을 돈의 금리는 어떻게 다른가 "를 점으로 연결한 것이 수익률 곡선입니다.
 * 이 지도가 있어야 미래에 들어올 돈의 현재 가치를 정확히 계산할 수 있으며,
 * 금리 변동 시나리오에 따른 자산 가치 변화를 정밀하게 예측할 수 있습니다.
 */
@RestController
@RequestMapping("/api/v1/market-data/yield-curves")
@RequiredArgsConstructor
public class YieldCurveController {

        private final YieldCurveService yieldCurveService;

        /**
         * 신규 수익률 곡선 및 포인트 정보 생성.
         */
        @PostMapping
        public ResponseEntity<ApiResponse<YieldCurveResponse>> create(@Valid @RequestBody YieldCurveRequest request) {
                YieldCurve saved = yieldCurveService.createCurve(
                                YieldCurve.builder()
                                                .curveName(request.getCurveName())
                                                .baseDate(request.getBaseDate())
                                                .currency(request.getCurrency().name())
                                                .curveDescription(request.getDescription())
                                                .build(),
                                request.getPoints() == null ? List.of()
                                                : request.getPoints().stream()
                                                                .map(point -> YieldCurvePoint.builder()
                                                                                .tenorCode(point.getTenorLabel() == null
                                                                                                ? point.getTenor() + "M"
                                                                                                : point.getTenorLabel())
                                                                                .tenorYear(point.getTenor() == null
                                                                                                ? null
                                                                                                : point.getTenor() / 12.0d)
                                                                                .rateValue(point.getRate())
                                                                                .build())
                                                                .collect(Collectors.toList()));
                return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(toResponse(saved)));
        }

        /**
         * 곡선명과 기준일 기반 수익률 곡선 상세 정보 조회.
         */
        @GetMapping("/{curveName}")
        public ResponseEntity<ApiResponse<YieldCurveResponse>> getById(
                        @PathVariable String curveName,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
                return ResponseEntity.ok(ApiResponse
                                .success(yieldCurveService.getCurve(curveName, baseDate).map(this::toResponse)
                                                .orElseThrow()));
        }

        /**
         * 특정 기준일의 모든 수익률 곡선 리스트 조회.
         */
        @GetMapping("/by-date")
        public ResponseEntity<ApiResponse<List<YieldCurveResponse>>> getByDate(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
                return ResponseEntity.ok(ApiResponse.success(yieldCurveService.getCurvesByDate(baseDate).stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList())));
        }

        /**
         * 특정 통화(Currency)별 수익률 곡선 리스트 조회.
         */
        @GetMapping("/by-currency")
        public ResponseEntity<ApiResponse<List<YieldCurveResponse>>> getByCurrency(
                        @RequestParam CurrencyCode currency) {
                return ResponseEntity.ok(ApiResponse.success(yieldCurveService.getCurvesByCurrency(currency.name()).stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList())));
        }

        /**
         * 명칭 기반 최신 수익률 곡선 조회.
         */
        @GetMapping("/latest/{curveName}")
        public ResponseEntity<ApiResponse<YieldCurveResponse>> getLatest(@PathVariable String curveName) {
                return ResponseEntity.ok(ApiResponse.success(
                                yieldCurveService.getLatestCurve(curveName).map(this::toResponse).orElseThrow()));
        }

        private YieldCurveResponse toResponse(YieldCurve curve) {
                return YieldCurveResponse.builder()
                                .curveName(curve.getCurveName())
                                .baseDate(curve.getBaseDate())
                                .currency(curve.getCurrency() == null ? null
                                                : CurrencyCode.valueOf(curve.getCurrency()))
                                .description(curve.getCurveDescription())
                                .isActive(true)
                                .points(curve.getPoints() == null ? List.of()
                                                : curve.getPoints().stream()
                                                                .map(point -> YieldCurveResponse.PointResponse.builder()
                                                                                .tenor(toTenorMonths(point))
                                                                                .tenorLabel(point.getTenorCode())
                                                                                .rate(point.getRateValue())
                                                                                .build())
                                                                .collect(Collectors.toList()))
                                .build();
        }

        private Integer toTenorMonths(YieldCurvePoint point) {
                return point.getTenorYear() == null ? null : (int) Math.round(point.getTenorYear() * 12.0d);
        }
}


