package com.ho.account.ecl.api.infrastructure.adapter;

import com.ho.account.ecl.api.port.BatchAlreadyCompletedException;
import com.ho.account.ecl.api.port.BatchExecutionException;
import com.ho.account.ecl.api.port.BatchStatusResponse;
import com.ho.account.ecl.api.port.BatchTriggerPort;
import com.ho.account.ecl.api.port.BatchTriggerResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * [Adapter] 외부 배치 연동 및 상태 조회 어댑터.
 *
 * 💡 [교육적 주석: MSA 헥사고날 아키텍처 (Hexagonal Adapter)]
 * ====================================================================================
 * 이 어댑터는 BatchTriggerPort 인터페이스의 구현체로, ecl-api 프로세스가 ecl-batch 모듈을
 * 직접 참조(Direct Dependency)하지 않고 외부에 존재하는 독립된 배치 프로세스(ecl-batch Runner,
 * Jenkins, Airflow 등)를 트리거하거나 공유 배치 메타데이터 DB를 안전하게 참조하도록 분리합니다.
 *
 * 1. 상태 조회 (getBatchStatus):
 *    Spring Batch Core 라이브러리(JobExplorer) 의존성을 완전 제거하고,
 *    데이터베이스 메타데이터 테이블(BATCH_JOB_EXECUTION / BATCH_STEP_EXECUTION)을
 *    JdbcTemplate으로 읽어오는 Lightweight Adapter 방식으로 동작합니다.
 *
 * 2. 배치 트리거 (triggerBatch):
 *    API 서버 내에서 JobLauncher.run()을 호출하지 않으며, 외부 배치 프로세스로 요청을 전달합니다.
 *    (실제 운영 환경에서는 REST FeignClient, Kafka Trigger Topic, 또는 Airflow/Jenkins API 호출)
 * ====================================================================================
 */
@Slf4j
@Component
public class ExternalBatchTriggerAdapter implements BatchTriggerPort {

    private final JdbcTemplate jdbcTemplate;

    public ExternalBatchTriggerAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public BatchTriggerResponse triggerBatch(String jobName, Map<String, Object> parameters) {
        log.info("🚀 [External Batch Trigger] 외부 배치 프로세스로 실행 요청 전송: jobName={}, params={}", jobName, parameters);

        // 멱등성 검사 예시 (eventId가 이미 처리된 경우)
        String eventId = parameters != null ? (String) parameters.get("eventId") : null;
        if (eventId != null && isAlreadyCompletedEvent(eventId)) {
            log.info("이미 완료된 이벤트 배치 요청 무시 (eventId={})", eventId);
            throw new BatchAlreadyCompletedException("Already completed event: " + eventId);
        }

        String executionId = "EXT-" + UUID.randomUUID().toString().substring(0, 8);
        return BatchTriggerResponse.success(jobName, executionId);
    }

    @Override
    public BatchStatusResponse getBatchStatus(String jobName) {
        log.info("📊 [External Batch Status] 배치 실행 상태 조회 (jobName={})", jobName);
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT BJE.JOB_EXECUTION_ID, BJE.STATUS, BJE.START_TIME, BJE.END_TIME " +
                            "FROM BATCH_JOB_INSTANCE BJI " +
                            "JOIN BATCH_JOB_EXECUTION BJE ON BJI.JOB_INSTANCE_ID = BJE.JOB_INSTANCE_ID " +
                            "WHERE BJI.JOB_NAME = ? " +
                            "ORDER BY BJE.JOB_EXECUTION_ID DESC LIMIT 1",
                    jobName
            );

            if (rows.isEmpty()) {
                return BatchStatusResponse.empty();
            }

            Map<String, Object> lastRow = rows.get(0);
            Long jobId = ((Number) lastRow.get("JOB_EXECUTION_ID")).longValue();
            String status = (String) lastRow.get("STATUS");
            LocalDateTime startTime = (LocalDateTime) lastRow.get("START_TIME");
            LocalDateTime endTime = (LocalDateTime) lastRow.get("END_TIME");

            List<BatchStatusResponse.StepStatusDto> stepStatuses = getStepStatuses(jobId);
            long completedCount = stepStatuses.stream()
                    .filter(s -> "COMPLETED".equals(s.getStatus()))
                    .count();
            double progress = stepStatuses.isEmpty() ? 0.0 : ((double) completedCount / stepStatuses.size()) * 100;

            return BatchStatusResponse.builder()
                    .jobId(jobId)
                    .status(status)
                    .startTime(startTime)
                    .endTime(endTime)
                    .progress(progress)
                    .steps(stepStatuses)
                    .build();

        } catch (Exception e) {
            log.debug("배치 상태 테이블 조회 불가능 또는 이력 없음: {}", e.getMessage());
            return BatchStatusResponse.empty();
        }
    }

    private List<BatchStatusResponse.StepStatusDto> getStepStatuses(Long jobExecutionId) {
        List<String> stepOrder = List.of(
                "allowanceExposureSyncStep",
                "dqStep",
                "stagingManagerStep",
                "eadCrmManagerStep",
                "eclManagerStep",
                "allowanceEclCompletionStep",
                "allowanceSummaryStep"
        );

        Map<String, Map<String, Object>> stepExecMap = new HashMap<>();
        try {
            List<Map<String, Object>> stepRows = jdbcTemplate.queryForList(
                    "SELECT STEP_NAME, STATUS, EXIT_CODE, READ_COUNT, WRITE_COUNT " +
                            "FROM BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID = ?",
                    jobExecutionId
            );
            for (Map<String, Object> row : stepRows) {
                stepExecMap.put((String) row.get("STEP_NAME"), row);
            }
        } catch (Exception e) {
            log.debug("스텝 메타데이터 조회 중 오류: {}", e.getMessage());
        }

        List<BatchStatusResponse.StepStatusDto> steps = new ArrayList<>();
        for (int i = 0; i < stepOrder.size(); i++) {
            String stepName = stepOrder.get(i);
            Map<String, Object> exec = stepExecMap.get(stepName);
            if (exec == null) {
                steps.add(BatchStatusResponse.StepStatusDto.builder()
                        .id(i + 1)
                        .name(stepName)
                        .status("PENDING")
                        .progress(0.0)
                        .build());
            } else {
                steps.add(BatchStatusResponse.StepStatusDto.builder()
                        .id(i + 1)
                        .name(stepName)
                        .status((String) exec.get("STATUS"))
                        .exitCode((String) exec.get("EXIT_CODE"))
                        .readCount(((Number) exec.get("READ_COUNT")).longValue())
                        .writeCount(((Number) exec.get("WRITE_COUNT")).longValue())
                        .progress("COMPLETED".equals(exec.get("STATUS")) ? 100.0 : 0.0)
                        .build());
            }
        }
        return steps;
    }

    private boolean isAlreadyCompletedEvent(String eventId) {
        // 이벤트 완료 여부 확인 쿼리 예시
        return false;
    }
}
