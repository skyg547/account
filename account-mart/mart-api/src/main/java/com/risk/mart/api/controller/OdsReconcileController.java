package com.risk.mart.api.controller;

import com.risk.mart.core.domain.ods.audit.entity.OdsDqAudit;
import com.risk.mart.core.domain.ods.audit.entity.OdsReconcileHist;
import com.risk.mart.core.domain.ods.audit.repository.OdsDqAuditRepository;
import com.risk.mart.core.domain.ods.audit.repository.OdsReconcileHistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [API] 데이터 대사 및 품질 검증 결과 조회 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/mart")
@RequiredArgsConstructor

public class OdsReconcileController {

    private final OdsReconcileHistRepository reconcileRepository;
    private final OdsDqAuditRepository dqAuditRepository;

    /**
     * 특정 기준일자의 데이터 대사(Reconciliation) 결과를 조회한다.
     */
    @GetMapping("/reconciliation")
    public Map<String, Object> getReconciliation(
            @RequestParam("baseDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {

        List<OdsReconcileHist> list = reconcileRepository.findByBaseDate(baseDate);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("data", list);
        response.put("timestamp", java.time.LocalDateTime.now());

        return response;
    }

    /**
     * 특정 기준일자의 데이터 품질(DQ) 감사 로그를 조회한다.
     */
    @GetMapping("/dq-audit")
    public Map<String, Object> getDqAudit(
            @RequestParam("baseDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {

        List<OdsDqAudit> list = dqAuditRepository.findByBaseDate(baseDate);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("data", list);
        response.put("timestamp", java.time.LocalDateTime.now());

        return response;
    }
}
