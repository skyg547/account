package com.accounting.system.expenditure.web;

import com.accounting.system.expenditure.domain.ExpenditureResolution;
import com.accounting.system.expenditure.service.ExpenditureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenditures")
public class ExpenditureController {

    private final ExpenditureService expenditureService;

    @Autowired
    public ExpenditureController(ExpenditureService expenditureService) {
        this.expenditureService = expenditureService;
    }

    // 결의서 생성
    @PostMapping
    public ResponseEntity<ExpenditureResolution> createResolution(@RequestBody ExpenditureResolution resolution) {
        try {
            ExpenditureResolution created = expenditureService.createResolution(resolution);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 승인 요청
    @PostMapping("/{id}/request")
    public ResponseEntity<Void> requestApproval(@PathVariable Long id) {
        try {
            expenditureService.requestApproval(id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 결의서 승인 (전표 자동 생성)
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveResolution(@PathVariable Long id) {
        try {
            expenditureService.approveResolution(id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 결의서 반려
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectResolution(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String reason = body.get("reason");
            expenditureService.rejectResolution(id, reason);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 결의서 조회 (기간별)
    @GetMapping
    public List<ExpenditureResolution> getResolutions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return expenditureService.getResolutionsByDate(startDate, endDate);
    }

    // 결의서 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<ExpenditureResolution> getResolution(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(expenditureService.getResolution(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
