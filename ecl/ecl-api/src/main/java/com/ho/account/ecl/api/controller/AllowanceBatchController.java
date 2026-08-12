package com.ho.account.ecl.api.controller;

import com.ho.account.ecl.api.port.BatchStatusResponse;
import com.ho.account.ecl.api.port.BatchTriggerPort;
import com.ho.account.ecl.api.port.BatchTriggerResponse;
import com.ho.account.ecl.core.application.service.calculation.EadBatchService;
import com.ho.account.ecl.core.domain.result.AllowanceEclResult;
import com.ho.account.shared.finance.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * [API Controller] IFRS 9 대손충당금 배치 모니터링 및 트리거 제어 컨트롤러.
 *
 * 💡 [교육적 주석: MSA 프로세스 & 리소스 격리 (Resource Isolation Architecture)]
 * ====================================================================================
 * 기존 MSA 아키텍처 결함 및 해결:
 *   - As-Is: ecl-api 모듈이 ecl-batch 모듈을 프로젝트 직결(Direct Project Dependency)로 참조하고,
 *     컨트롤러 내에서 Spring Batch의 JobLauncher / JobExplorer를 직접 주입받아 동일 JVM 내에서 배치를 실행했습니다.
 *     이로 인해 대용량 계산 작업이 진행되는 동안 API 서버의 메모리(OOM 발생 위험) 및 CPU, DB 커넥션이
 *     고갈되어 실시간 HTTP REST 서비스 품질이 심각하게 저하되었습니다.
 *
 *   - To-Be: ecl-api에서 Spring Batch 및 ecl-batch 모듈 의존성을 완전 제거했습니다.
 *     API 프로세스와 배치 프로세스의 '프로세스 및 리소스 격리(Resource & Process Isolation)'를 달성하였습니다.
 *     이 컨트롤러는 배치 실행기(JobLauncher)를 품는 대신, 계약된 포트(BatchTriggerPort)를 통해
 *     독립된 배치 프로세스로 비동기 실행을 위임하고 상태를 조회합니다.
 * ====================================================================================
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ifrs/allowance/batch")
public class AllowanceBatchController {

    // 외부 배치 워커/서비스 연동 및 상태 조회용 포트
    private final BatchTriggerPort batchTriggerPort;

    // EAD 산출 핵심 비즈니스 도메인 서비스 (ecl-core)
    private final EadBatchService eadBatchService;

    public AllowanceBatchController(
            BatchTriggerPort batchTriggerPort,
            EadBatchService eadBatchService) {
        this.batchTriggerPort = batchTriggerPort;
        this.eadBatchService = eadBatchService;
    }

    /**
     * 최근 배치 실행 상태 및 파이프라인 단계별 진행 상황을 조회한다.
     */
    @GetMapping("/status")
    public BatchStatusResponse getBatchStatus() {
        log.info("📊 [배치 모니터링] 파이프라인 상태 조회 요청 (External Port)");
        return batchTriggerPort.getBatchStatus("allowanceEclJob");
    }

    /**
     * 대손충당금(IFRS9) 배치를 외부 프로세스로 수동 트리거 요청한다.
     */
    @PostMapping("/run")
    public Map<String, String> runBatch(@RequestParam(required = false, defaultValue = "allowanceEclJob") String jobName) {
        log.info("🚀 [배치 실행 요청] IFRS 9 대손충당금 외부 배치 트리거 요청 (Job: {})", jobName);

        try {
            Map<String, Object> params = Map.of(
                    "baseDate", LocalDate.now().toString(),
                    "timestamp", System.currentTimeMillis()
            );
            BatchTriggerResponse response = batchTriggerPort.triggerBatch(jobName, params);
            return Map.of("message", response.getMessage(), "status", response.getStatus());
        } catch (Exception e) {
            log.error("❌ [배치 실행 요청 실패]", e);
            return Map.of("message", "배치 실행 요청 중 오류가 발생했습니다: " + e.getMessage(), "status", "FAILED");
        }
    }

    /**
     * [POST] EAD 산출 전용 계산을 실행한다.
     *
     * @param baseDate 기준일자 (예: 2026-04-18)
     * @return 산출된 EAD 결과 목록
     */
    @PostMapping("/run-ead")
    public ResponseEntity<ApiResponse<List<AllowanceEclResult>>> runEadBatch(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
        log.info("🚀 [EAD 산출 계산] {} 기준 EAD 산출 요청이 수신되었습니다.", baseDate);
        return ResponseEntity.ok(ApiResponse.success(eadBatchService.executeBatch(baseDate)));
    }
}
