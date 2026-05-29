package com.ho.account.ecl.api.controller;

import com.ho.account.ecl.core.application.service.calculation.EadBatchService;
import com.ho.account.ecl.core.domain.result.AllowanceEclResult;
import com.ho.account.shared.finance.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * [API] IFRS 9 대손충당금 배치 모니터링 및 실행 제어 컨트롤러.
 *
 * 💡 [초보자를 위한 개념 설명]
 * ============================================================
 * "배치(Batch)"란 은행의 야간 자동 결산처럼, 대량의 데이터를 한꺼번에 처리하는 작업입니다.
 * 이 컨트롤러는 배치의 '관제탑' 역할로:
 *   - GET /status → 배치가 현재 어느 단계까지 완료되었는지 조회합니다.
 *   - POST /run   → 배치를 수동으로 즉시 시작시킵니다. (자동 실행 실패 시 긴급 재수행)
 *   - POST /run-ead → EAD 산출 전용 배치를 실행합니다. (프론트엔드 대시보드 연동)
 * ============================================================
 *
 * 표준 파이프라인 순서:
 *   1. allowanceExposureSyncStep
 *   2. dqStep
 *   3. stagingManagerStep
 *   4. eadCrmManagerStep
 *   5. eclManagerStep
 *   6. allowanceEclCompletionStep
 *   7. allowanceSummaryStep
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ifrs/allowance/batch")
@RequiredArgsConstructor
public class AllowanceBatchController {

    // Spring Batch의 Job을 실행시키는 런처. "Job 시작!"을 명령하는 역할입니다.
    private final JobLauncher jobLauncher;

    // 실행 중이거나 완료된 배치의 이력(History)을 조회하는 탐색기입니다.
    private final JobExplorer jobExplorer;

    // EAD 산출 서비스 - /run-ead 엔드포인트에서 호출합니다.
    private final EadBatchService eadBatchService;

    // Spring ApplicationContext: 문자열(jobName)로 Bean을 찾을 때 사용합니다.
    private final org.springframework.context.ApplicationContext context;

    // @Qualifier: 같은 타입(Job)의 Bean이 여러 개일 때 어떤 것을 쓸지 이름으로 지정합니다.
    @org.springframework.beans.factory.annotation.Qualifier("allowanceEclJob")
    private final Job allowanceEclJob;

    /**
     * 최근 배치 실행 상태 및 8단계별 진행 상황을 조회한다.
     */
    @GetMapping("/status")
    public Map<String, Object> getBatchStatus() {
        log.info("📊 [배치 모니터링] 파이프라인 상태 조회 요청");

        List<JobInstance> instances = jobExplorer.getJobInstances("allowanceEclJob", 0, 1);
        if (instances.isEmpty()) {
            return createEmptyStatusResponse();
        }

        JobInstance lastInstance = instances.get(0);
        List<JobExecution> executions = jobExplorer.getJobExecutions(lastInstance);
        if (executions.isEmpty()) {
            return createEmptyStatusResponse();
        }
        JobExecution lastExecution = executions.get(0);

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", lastExecution.getId());
        response.put("status", lastExecution.getStatus().toString());
        response.put("startTime", lastExecution.getStartTime());
        response.put("endTime", lastExecution.getEndTime());

        // 8단계별 상태 매핑
        List<Map<String, Object>> stepStatuses = mapStepStatuses(lastExecution);
        response.put("steps", stepStatuses);

        // 전체 진척률 계산 (완료된 스텝 수 / 전체 7단계)
        long completedSteps = stepStatuses.stream()
                .filter(s -> "COMPLETED".equals(s.get("status")))
                .count();
        response.put("progress", (double) completedSteps / 7 * 100);

        return response;
    }

    /**
     * 대손충당금(IFRS9) 배치를 수동으로 실행한다. (또는 특정 개별 Job 실행)
     */
    @PostMapping("/run")
    public Map<String, String> runBatch(@RequestParam(required = false, defaultValue = "allowanceEclJob") String jobName) {
        log.info("🚀 [배치 실행] IFRS 9 대손충당금 파이프라인 수동 실행 요청 (Job: {})", jobName);

        try {
            Job targetJob = context.getBean(jobName, Job.class);
            JobParameters params = new JobParametersBuilder()
                    .addString("baseDate", java.time.LocalDate.now().toString()) // yyyy-MM-dd 형식 유지
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(targetJob, params);
            return Map.of("message", jobName + " 배치가 성공적으로 시작되었습니다.", "status", "STARTED");
        } catch (Exception e) {
            log.error("❌ [배치 실행 실패]", e);
            return Map.of("message", "배치 실행 중 오류가 발생했습니다: " + e.getMessage(), "status", "FAILED");
        }
    }

    private List<Map<String, Object>> mapStepStatuses(JobExecution execution) {
        // 실제 배치에 정의된 Step 이름 순서
        List<String> stepOrder = List.of(
                "allowanceExposureSyncStep",
                "dqStep",
                "stagingManagerStep",
                "eadCrmManagerStep",
                "eclManagerStep",
                "allowanceEclCompletionStep",
                "allowanceSummaryStep"
        );

        Map<String, StepExecution> executionMap = execution.getStepExecutions().stream()
                .collect(Collectors.toMap(StepExecution::getStepName, s -> s));

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < stepOrder.size(); i++) {
            String stepName = stepOrder.get(i);
            StepExecution stepExec = executionMap.get(stepName);

            Map<String, Object> stepMap = new HashMap<>();
            stepMap.put("id", i + 1);
            stepMap.put("name", stepName);
            
            if (stepExec == null) {
                stepMap.put("status", "PENDING");
                stepMap.put("progress", 0);
            } else {
                stepMap.put("status", stepExec.getStatus().toString());
                stepMap.put("exitCode", stepExec.getExitStatus().getExitCode());
                stepMap.put("readCount", stepExec.getReadCount());
                stepMap.put("writeCount", stepExec.getWriteCount());
            }
            result.add(stepMap);
        }
        return result;
    }

    /**
     * [POST] EAD 산출 전용 배치를 실행한다.
     *
     * 💡 [왕초보 가이드]
     * 프론트엔드의 'EAD 검증' 페이지에서 "일일 배치 실행" 버튼을 누르면 이 엔드포인트가 호출됩니다.
     *   - EAD(부도시 익스포저) = 현재 잔액 + 미사용 한도 × CCF 공식으로 대량 계산을 수행합니다.
     *
     * @param baseDate 기준일자 (예: 2026-04-18)
     * @return 산출된 EAD 결과 목록
     */
    @PostMapping("/run-ead")
    public ResponseEntity<ApiResponse<List<AllowanceEclResult>>> runEadBatch(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
        log.info("🚀 [EAD 배치 실행] {} 기준 EAD 산출 요청이 수신되었습니다.", baseDate);
        // EadBatchService: credit-core의 비즈니스 로직에서 실제 EAD 계산을 수행합니다.
        return ResponseEntity.ok(ApiResponse.success(eadBatchService.executeBatch(baseDate)));
    }

    /**
     * 배치 이력이 없을 때 반환하는 기본 응답을 생성한다.
     *
     * 💡 [왕초보 가이드]
     * 배치가 한 번도 실행된 적이 없으면, JobExplorer는 빈 목록을 반환합니다.
     * 이 경우 '데이터 없음' 상태를 클라이언트에게 명확하게 알려주기 위한 응답을 만들어 줍니다.
     */
    private Map<String, Object> createEmptyStatusResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "NO_HISTORY");   // 배치 실행 이력 없음
        response.put("steps", Collections.emptyList()); // 빈 단계 목록
        response.put("progress", 0);            // 진행률 0%
        return response;
    }
}

