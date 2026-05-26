package com.risk.mart.api.controller;

import com.risk.common.entity.RiskAuditLog;
import com.risk.mart.core.domain.governance.repository.RiskAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [거버넌스] 시스템 감사 및 관제 컨트롤러 (Governance Controller)
 *
 * 💡 [초보자를 위한 개념 설명]
 * 거버넌스(Governance)는 시스템이 투명하고 안전하게 운영되는지 감시하는 '관리 체계'입니다.
 * 리스크 산출은 은행의 자본과 직결되므로 "누가, 언제, 어떤 시나리오로 계산을 실행했는지"를
 * 기록(Audit Log)하고, 시스템의 건강 상태(Health)를 체크하는 것이 매우 중요합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/mart/governance")
@RequiredArgsConstructor
public class GovernanceController {

    private final RiskAuditLogRepository auditLogRepository;

    /**
     * 관제 센터를 위한 최근 시스템 감사 로그를 조회한다.
     */
    @GetMapping("/audit-logs")
    public List<RiskAuditLog> getRecentLogs() {
        return auditLogRepository.findTop10ByOrderByCreatedAtDesc();
    }

    /**
     * 리스크 시스템 전반의 서비스 가동 상태(Health Snapshot)를 조회한다.
     */
    @GetMapping("/health")
    public List<Map<String, Object>> getServiceHealth() {
        List<Map<String, Object>> services = new ArrayList<>();

        services.add(createStatus("Discovery-Service", "정상(UP)", "0ms"));
        services.add(createStatus("Gateway-Service", "정상(UP)", "2ms"));
        services.add(createStatus("Credit-Risk-Service", "정상(UP)", "15ms"));
        services.add(createStatus("Interest-Rate-Risk-Service", "정상(UP)", "8ms"));
        services.add(createStatus("Risk-Data-Mart-Service", "정상(UP)", "0ms"));

        return services;
    }

    /**
     * 시스템 통합 리스크 요약 스냅샷을 조회한다.
     */
    @GetMapping("/snapshot")
    public Map<String, Object> getGovernanceSnapshot() {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("lastCalculationDate", LocalDateTime.now());
        snapshot.put("complianceStatus", "정상(COMPLIANT)");
        snapshot.put("totalExposure", 2485000000L);
        snapshot.put("regulatoryCapital", 215000000L);
        snapshot.put("capitalAdequacyRatio", 12.45);
        snapshot.put("activeAlarms", 2);
        return snapshot;
    }

    private Map<String, Object> createStatus(String name, String status, String latency) {
        Map<String, Object> m = new HashMap<>();
        m.put("serviceName", name);
        m.put("status", status);
        m.put("latency", latency);
        m.put("lastCheck", LocalDateTime.now());
        return m;
    }

    /**
     * 데모용 테스트 로그 데이터를 생성한다.
     */
    @PostMapping("/audit-logs/seed")
    @SuppressWarnings("null")
    public RiskAuditLog seedLog() {
        RiskAuditLog log = RiskAuditLog.builder()
                .serviceName("CREDIT_RISK")
                .actionType("STRESS_TEST")
                .status("SUCCESS")
                .executionParam("{\"scenario\":\"SEVERE_CRISIS\",\"baseDate\":\"2026-04-13\"}")
                .executedBy("admin_risk")
                .durationMs(1450L)
                .build();
        auditLogRepository.save(log);
        return log;
    }
}
