package com.ho.account.reconciliation.web;

import com.ho.account.reconciliation.domain.ReconciliationType;
import com.ho.account.reconciliation.dto.ReconciliationRequestDto;
import com.ho.account.reconciliation.dto.ReconciliationResponseDto;
import com.ho.account.reconciliation.dto.VarianceDto;
import com.ho.account.reconciliation.service.ReconciliationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @Autowired
    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    // ===== 대사 실행 엔드포인트 =====

    @PostMapping("/run")
    public ResponseEntity<ReconciliationResponseDto> runReconciliation(
            @RequestBody ReconciliationRequestDto request) {
        ReconciliationResponseDto result = reconciliationService.executeReconciliation(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/run/source-standard")
    public ResponseEntity<ReconciliationResponseDto> runSourceStandard(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reconciliationDate,
            @RequestParam(required = false) String runBy) {
        ReconciliationResponseDto result = reconciliationService.performSourceStandardReconciliation(reconciliationDate,
                runBy);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/run/account-totals")
    public ResponseEntity<ReconciliationResponseDto> runAccountTotals(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reconciliationDate,
            @RequestParam(required = false) String runBy) {
        ReconciliationResponseDto result = reconciliationService.performAccountTotalsReconciliation(reconciliationDate,
                runBy);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/run/bank-account")
    public ResponseEntity<ReconciliationResponseDto> runBankAccount(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reconciliationDate,
            @RequestParam(required = false) String runBy) {
        ReconciliationResponseDto result = reconciliationService.performBankAccountReconciliation(reconciliationDate,
                runBy);
        return ResponseEntity.ok(result);
    }

    // ===== 조회 엔드포인트 =====

    @GetMapping("/results")
    public ResponseEntity<List<ReconciliationResponseDto>> getAllResults() {
        return ResponseEntity.ok(reconciliationService.getAllReconciliationResults());
    }

    @GetMapping("/results/{id}")
    public ResponseEntity<ReconciliationResponseDto> getResultById(@PathVariable Long id) {
        return ResponseEntity.ok(reconciliationService.getReconciliationResultById(id));
    }

    @GetMapping("/results/type/{type}")
    public ResponseEntity<List<ReconciliationResponseDto>> getResultsByType(@PathVariable ReconciliationType type) {
        return ResponseEntity.ok(reconciliationService.getReconciliationResultsByType(type));
    }

    @GetMapping("/results/date/{date}")
    public ResponseEntity<List<ReconciliationResponseDto>> getResultsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(reconciliationService.getReconciliationResultsByDate(date));
    }

    @GetMapping("/results/{resultId}/variances")
    public ResponseEntity<List<VarianceDto>> getVariancesByResult(@PathVariable Long resultId) {
        return ResponseEntity.ok(reconciliationService.getVariancesByReconciliationResult(resultId));
    }

    @GetMapping("/variances/open")
    public ResponseEntity<List<VarianceDto>> getOpenVariances() {
        return ResponseEntity.ok(reconciliationService.getOpenVariances());
    }

    // ===== 차이 해소 엔드포인트 =====

    @PostMapping("/variances/{varianceId}/resolve")
    public ResponseEntity<VarianceDto> resolveVariance(
            @PathVariable Long varianceId,
            @RequestBody Map<String, Object> body) {
        Long journalEntryId = Long.valueOf(body.get("journalEntryId").toString());
        String resolvedBy = (String) body.get("resolvedBy");
        VarianceDto result = reconciliationService.resolveVarianceWithAdjustment(varianceId, journalEntryId,
                resolvedBy);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/variances/{varianceId}/ignore")
    public ResponseEntity<VarianceDto> ignoreVariance(
            @PathVariable Long varianceId,
            @RequestBody Map<String, String> body) {
        String resolvedBy = body.get("resolvedBy");
        VarianceDto result = reconciliationService.ignoreVariance(varianceId, resolvedBy);
        return ResponseEntity.ok(result);
    }
}
