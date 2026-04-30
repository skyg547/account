package com.ho.account.risk.web;

import com.ho.account.risk.application.port.in.RiskAssessmentUseCase;
import com.ho.account.risk.dto.RiskWeightedAssetDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/risk/assessment")
public class RiskAssessmentController {

    private final RiskAssessmentUseCase riskAssessmentUseCase;

    public RiskAssessmentController(RiskAssessmentUseCase riskAssessmentUseCase) {
        this.riskAssessmentUseCase = riskAssessmentUseCase;
    }

    @PostMapping("/generate-exposures")
    public ResponseEntity<Void> generateExposures(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
        riskAssessmentUseCase.generateExposures(baseDate);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/calculate-rwa")
    public ResponseEntity<Void> calculateRwa(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate,
            @RequestParam(defaultValue = "SA") String approachType) {
        riskAssessmentUseCase.calculateRwa(baseDate, approachType);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/rwa-results")
    public ResponseEntity<List<RiskWeightedAssetDto>> getRwaResults(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(riskAssessmentUseCase.getRwaResults(startDate, endDate).stream()
                .map(RiskWeightedAssetDto::fromEntity)
                .collect(Collectors.toList()));
    }
}
