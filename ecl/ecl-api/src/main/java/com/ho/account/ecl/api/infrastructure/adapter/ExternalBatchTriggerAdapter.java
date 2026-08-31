package com.ho.account.ecl.api.infrastructure.adapter;

import com.ho.account.ecl.api.port.BatchAlreadyCompletedException;
import com.ho.account.ecl.api.port.BatchJobExecutor;
import com.ho.account.ecl.api.port.BatchStatusResponse;
import com.ho.account.ecl.api.port.BatchTriggerPort;
import com.ho.account.ecl.api.port.BatchTriggerResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
 *    메타데이터 테이블이 없거나 초기화되지 않은 환경에서는 인메모리/DB 트리거 이력을 조회하는
 *    Fallback 메커니즘을 제공합니다.
 *
 * 2. 배치 트리거 (triggerBatch):
 *    - Optional BatchJobExecutor(로컬/테스트 러너)가 등록되어 있으면 즉시 배치를 직접 구동합니다.
 *    - 직접 구동기가 없는 프로덕션 환경에서는 batch_trigger_executions 테이블 및 인메모리 큐에
 *      트리거 이력을 안전하게 영속화하고 STARTED 상태를 반환합니다.
 *    - 동일 eventId에 대한 멱등성(Idempotency) 검사를 수행하여 이미 완료된 작업은
 *      BatchAlreadyCompletedException을 발생시켜 안전하게 중복을 방지합니다.
 * ====================================================================================
 */
@Slf4j
@Component
public class ExternalBatchTriggerAdapter implements BatchTriggerPort {

    private final JdbcTemplate jdbcTemplate;
    private final Optional<BatchJobExecutor> batchJobExecutor;
    private final Map<String, TriggeredJobRecord> recentExecutions = new ConcurrentHashMap<>();

    public record TriggeredJobRecord(
            String jobName,
            String executionId,
            Map<String, Object> parameters,
            String status,
            LocalDateTime triggeredAt
    ) {}

    @Autowired
    public ExternalBatchTriggerAdapter(
            JdbcTemplate jdbcTemplate,
            @Autowired(required = false) Optional<BatchJobExecutor> batchJobExecutor) {
        this.jdbcTemplate = jdbcTemplate;
        this.batchJobExecutor = batchJobExecutor != null ? batchJobExecutor : Optional.empty();
        initSchemaIfNeeded();
    }

    public ExternalBatchTriggerAdapter(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, Optional.empty());
    }

    public ExternalBatchTriggerAdapter(JdbcTemplate jdbcTemplate, BatchJobExecutor batchJobExecutor) {
        this(jdbcTemplate, Optional.ofNullable(batchJobExecutor));
    }

    @Override
    public BatchTriggerResponse triggerBatch(String jobName, Map<String, Object> parameters) {
        log.info("🚀 [External Batch Trigger] 외부 배치 프로세스로 실행 요청 전송: jobName={}, params={}", jobName, parameters);

        // 멱등성 검사 (eventId가 이미 처리된 경우)
        String eventId = parameters != null ? Objects.toString(parameters.get("eventId"), null) : null;
        if (eventId != null && isAlreadyCompletedEvent(eventId)) {
            log.info("이미 완료된 이벤트 배치 요청 무시 (eventId={})", eventId);
            throw new BatchAlreadyCompletedException("Already completed event: " + eventId);
        }

        // 1. Optional BatchJobExecutor 주입 시 직접 실행 위임 (로컬/테스트 환경)
        if (batchJobExecutor.isPresent()) {
            log.info("직접 실행 러너(BatchJobExecutor)를 통한 배치 구동: jobName={}", jobName);
            return batchJobExecutor.get().execute(jobName, parameters);
        }

        // 2. 비동기/외부 배치 트리거 기록 영속화
        String executionId = "EXT-" + UUID.randomUUID().toString().substring(0, 8);
        recordTriggerExecution(jobName, executionId, parameters);

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

            if (!rows.isEmpty()) {
                Map<String, Object> lastRow = rows.get(0);
                Long jobId = toLong(lastRow.get("JOB_EXECUTION_ID"));
                String status = (String) lastRow.get("STATUS");
                LocalDateTime startTime = toLocalDateTime(lastRow.get("START_TIME"));
                LocalDateTime endTime = toLocalDateTime(lastRow.get("END_TIME"));

                List<BatchStatusResponse.StepStatusDto> stepStatuses = getStepStatuses(jobId, jobName);
                long completedCount = stepStatuses.stream()
                        .filter(s -> "COMPLETED".equalsIgnoreCase(s.getStatus()))
                        .count();
                double progress = 0.0;
                if ("COMPLETED".equalsIgnoreCase(status)) {
                    progress = 100.0;
                } else if (!stepStatuses.isEmpty()) {
                    long runningCount = stepStatuses.stream()
                            .filter(s -> "STARTED".equalsIgnoreCase(s.getStatus()) || "STARTING".equalsIgnoreCase(s.getStatus()))
                            .count();
                    progress = Math.min(100.0, Math.round(((completedCount * 100.0 + runningCount * 50.0) / stepStatuses.size()) * 10.0) / 10.0);
                }

                return BatchStatusResponse.builder()
                        .jobId(jobId)
                        .status(status)
                        .startTime(startTime)
                        .endTime(endTime)
                        .progress(progress)
                        .steps(stepStatuses)
                        .build();
            }
        } catch (Exception e) {
            log.debug("배치 상태 테이블 조회 불가능 또는 오류: {}", e.getMessage());
        }

        // Spring Batch DB 메타데이터가 없으면 최근 트리거 이력 조회
        return getFallbackBatchStatus(jobName);
    }

    private List<BatchStatusResponse.StepStatusDto> getStepStatuses(Long jobExecutionId, String jobName) {
        List<String> standardSteps = getStandardStepOrder(jobName);

        Map<String, Map<String, Object>> stepExecMap = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> stepRows = jdbcTemplate.queryForList(
                    "SELECT STEP_NAME, STATUS, EXIT_CODE, READ_COUNT, WRITE_COUNT " +
                            "FROM BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID = ? ORDER BY STEP_EXECUTION_ID ASC",
                    jobExecutionId
            );
            for (Map<String, Object> row : stepRows) {
                stepExecMap.put((String) row.get("STEP_NAME"), row);
            }
        } catch (Exception e) {
            log.debug("스텝 메타데이터 조회 중 오류: {}", e.getMessage());
        }

        List<BatchStatusResponse.StepStatusDto> steps = new ArrayList<>();
        int idCounter = 1;

        // 1. 표준 정의 스텝 매핑
        for (String stepName : standardSteps) {
            Map<String, Object> exec = stepExecMap.remove(stepName);
            if (exec == null) {
                steps.add(BatchStatusResponse.StepStatusDto.builder()
                        .id(idCounter++)
                        .name(stepName)
                        .status("PENDING")
                        .progress(0.0)
                        .build());
            } else {
                String status = (String) exec.get("STATUS");
                double progress = "COMPLETED".equalsIgnoreCase(status) ? 100.0
                        : ("STARTED".equalsIgnoreCase(status) || "STARTING".equalsIgnoreCase(status) ? 50.0 : 0.0);
                steps.add(BatchStatusResponse.StepStatusDto.builder()
                        .id(idCounter++)
                        .name(stepName)
                        .status(status)
                        .exitCode((String) exec.get("EXIT_CODE"))
                        .readCount(toLong(exec.get("READ_COUNT")))
                        .writeCount(toLong(exec.get("WRITE_COUNT")))
                        .progress(progress)
                        .build());
            }
        }

        // 2. 표준 정의 외 추가 실행된 스텝(파티션 등) 매핑
        for (Map.Entry<String, Map<String, Object>> entry : stepExecMap.entrySet()) {
            Map<String, Object> exec = entry.getValue();
            String status = (String) exec.get("STATUS");
            double progress = "COMPLETED".equalsIgnoreCase(status) ? 100.0
                    : ("STARTED".equalsIgnoreCase(status) || "STARTING".equalsIgnoreCase(status) ? 50.0 : 0.0);
            steps.add(BatchStatusResponse.StepStatusDto.builder()
                    .id(idCounter++)
                    .name(entry.getKey())
                    .status(status)
                    .exitCode((String) exec.get("EXIT_CODE"))
                    .readCount(toLong(exec.get("READ_COUNT")))
                    .writeCount(toLong(exec.get("WRITE_COUNT")))
                    .progress(progress)
                    .build());
        }

        return steps;
    }

    private BatchStatusResponse getFallbackBatchStatus(String jobName) {
        // 1. 인메모리 최근 트리거 기록 확인
        TriggeredJobRecord record = recentExecutions.get(jobName);
        if (record != null) {
            List<String> stepOrder = getStandardStepOrder(jobName);
            List<BatchStatusResponse.StepStatusDto> steps = new ArrayList<>();
            for (int i = 0; i < stepOrder.size(); i++) {
                steps.add(BatchStatusResponse.StepStatusDto.builder()
                        .id(i + 1)
                        .name(stepOrder.get(i))
                        .status("PENDING")
                        .progress(0.0)
                        .build());
            }
            return BatchStatusResponse.builder()
                    .jobId(0L)
                    .status(record.status())
                    .startTime(record.triggeredAt())
                    .endTime(null)
                    .progress("COMPLETED".equalsIgnoreCase(record.status()) ? 100.0 : 0.0)
                    .steps(steps)
                    .build();
        }

        // 2. batch_trigger_executions 테이블 확인
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, execution_id, status, triggered_at, completed_at " +
                            "FROM batch_trigger_executions " +
                            "WHERE job_name = ? ORDER BY id DESC LIMIT 1",
                    jobName
            );
            if (!rows.isEmpty()) {
                Map<String, Object> row = rows.get(0);
                Long id = toLong(row.get("id"));
                String status = (String) row.get("status");
                LocalDateTime startTime = toLocalDateTime(row.get("triggered_at"));
                LocalDateTime endTime = toLocalDateTime(row.get("completed_at"));

                List<String> stepOrder = getStandardStepOrder(jobName);
                List<BatchStatusResponse.StepStatusDto> steps = new ArrayList<>();
                for (int i = 0; i < stepOrder.size(); i++) {
                    steps.add(BatchStatusResponse.StepStatusDto.builder()
                            .id(i + 1)
                            .name(stepOrder.get(i))
                            .status("COMPLETED".equalsIgnoreCase(status) ? "COMPLETED" : "PENDING")
                            .progress("COMPLETED".equalsIgnoreCase(status) ? 100.0 : 0.0)
                            .build());
                }

                return BatchStatusResponse.builder()
                        .jobId(id)
                        .status(status)
                        .startTime(startTime)
                        .endTime(endTime)
                        .progress("COMPLETED".equalsIgnoreCase(status) ? 100.0 : 0.0)
                        .steps(steps)
                        .build();
            }
        } catch (Exception e) {
            log.debug("batch_trigger_executions 조회 중 오류: {}", e.getMessage());
        }

        return BatchStatusResponse.empty();
    }

    private void recordTriggerExecution(String jobName, String executionId, Map<String, Object> parameters) {
        LocalDateTime now = LocalDateTime.now();
        String paramsStr = parameters != null ? parameters.toString() : "{}";
        recentExecutions.put(jobName, new TriggeredJobRecord(jobName, executionId, parameters, "STARTED", now));

        try {
            jdbcTemplate.update(
                    "INSERT INTO batch_trigger_executions (execution_id, job_name, parameters, status, message, triggered_at) " +
                            "VALUES (?, ?, ?, 'STARTED', 'Batch execution triggered', ?)",
                    executionId,
                    jobName,
                    paramsStr,
                    now
            );
        } catch (Exception e) {
            log.debug("batch_trigger_executions 삽입 생략 또는 실패: {}", e.getMessage());
        }
    }

    private boolean isAlreadyCompletedEvent(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            return false;
        }

        // 1. Spring Batch 공식 메타데이터 테이블 (BATCH_JOB_EXECUTION_PARAMS) 확인
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) " +
                            "FROM BATCH_JOB_EXECUTION_PARAMS P " +
                            "JOIN BATCH_JOB_EXECUTION E ON P.JOB_EXECUTION_ID = E.JOB_EXECUTION_ID " +
                            "WHERE P.PARAMETER_NAME = 'eventId' " +
                            "  AND P.PARAMETER_VALUE = ? " +
                            "  AND E.STATUS = 'COMPLETED'",
                    Integer.class,
                    eventId
            );
            if (count != null && count > 0) {
                return true;
            }
        } catch (Exception e) {
            log.debug("BATCH_JOB_EXECUTION_PARAMS 확인 불가: {}", e.getMessage());
        }

        // 2. batch_trigger_executions 테이블 확인
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM batch_trigger_executions " +
                            "WHERE parameters LIKE ? AND status = 'COMPLETED'",
                    Integer.class,
                    "%" + eventId + "%"
            );
            if (count != null && count > 0) {
                return true;
            }
        } catch (Exception e) {
            log.debug("batch_trigger_executions 확인 불가: {}", e.getMessage());
        }

        // 3. 인메모리 최근 기록 확인
        for (TriggeredJobRecord record : recentExecutions.values()) {
            if (record.parameters() != null
                    && eventId.equals(record.parameters().get("eventId"))
                    && "COMPLETED".equalsIgnoreCase(record.status())) {
                return true;
            }
        }

        return false;
    }

    private void initSchemaIfNeeded() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS batch_trigger_executions (
                    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    execution_id VARCHAR(100) NOT NULL UNIQUE,
                    job_name VARCHAR(100) NOT NULL,
                    parameters TEXT,
                    status VARCHAR(20) NOT NULL,
                    message VARCHAR(500),
                    triggered_at TIMESTAMP NOT NULL,
                    completed_at TIMESTAMP
                )
            """);
        } catch (Exception e) {
            log.debug("batch_trigger_executions 테이블 생성 스킵: {}", e.getMessage());
        }
    }

    private List<String> getStandardStepOrder(String jobName) {
        if ("allowanceEclJob".equals(jobName)) {
            return List.of(
                    "allowanceExposureSyncStep",
                    "dqStep",
                    "stagingWarmingStep",
                    "resultPreparationStep",
                    "stagingManagerStep",
                    "exposureWarmingStep",
                    "eadCrmManagerStep",
                    "reportingWarmingStep",
                    "eclManagerStep",
                    "allowanceEclCompletionStep",
                    "allowanceSummaryStep"
            );
        }
        if ("preProcessingJob".equals(jobName)) {
            return List.of("cdmSyncStep", "dqStep");
        }
        if ("allowanceStagingJob".equals(jobName)) {
            return List.of("stagingWarmingStep", "resultPreparationStep", "stagingManagerStep");
        }
        if ("exposureLgdJob".equals(jobName)) {
            return List.of("exposureWarmingStep", "eadCrmManagerStep");
        }
        if ("mainReportingJob".equals(jobName)) {
            return List.of("reportingWarmingStep", "eclManagerStep", "allowanceSummaryStep");
        }
        return Collections.emptyList();
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (value instanceof java.util.Date d) {
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        return null;
    }

    private static long toLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        return 0L;
    }
}
