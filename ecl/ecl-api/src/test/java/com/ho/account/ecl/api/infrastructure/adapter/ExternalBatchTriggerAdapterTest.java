package com.ho.account.ecl.api.infrastructure.adapter;

import com.ho.account.ecl.api.port.BatchAlreadyCompletedException;
import com.ho.account.ecl.api.port.BatchJobExecutor;
import com.ho.account.ecl.api.port.BatchStatusResponse;
import com.ho.account.ecl.api.port.BatchTriggerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExternalBatchTriggerAdapterTest {

    private JdbcTemplate jdbcTemplate;
    private ExternalBatchTriggerAdapter adapter;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("testdb-" + System.nanoTime())
                .build();
        jdbcTemplate = new JdbcTemplate(dataSource);
        adapter = new ExternalBatchTriggerAdapter(jdbcTemplate);
    }

    private void createSpringBatchMetadataTables() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS BATCH_JOB_INSTANCE (
                JOB_INSTANCE_ID BIGINT NOT NULL PRIMARY KEY,
                VERSION BIGINT,
                JOB_NAME VARCHAR(100) NOT NULL,
                JOB_KEY VARCHAR(32) NOT NULL
            );
            CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION (
                JOB_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
                VERSION BIGINT,
                JOB_INSTANCE_ID BIGINT NOT NULL,
                CREATE_TIME TIMESTAMP NOT NULL,
                START_TIME TIMESTAMP,
                END_TIME TIMESTAMP,
                STATUS VARCHAR(10),
                EXIT_CODE VARCHAR(2500),
                EXIT_MESSAGE VARCHAR(2500),
                LAST_UPDATED TIMESTAMP
            );
            CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION_PARAMS (
                JOB_EXECUTION_ID BIGINT NOT NULL,
                PARAMETER_NAME VARCHAR(100) NOT NULL,
                PARAMETER_TYPE VARCHAR(100) NOT NULL,
                PARAMETER_VALUE VARCHAR(2500),
                IDENTIFYING CHAR(1) NOT NULL
            );
            CREATE TABLE IF NOT EXISTS BATCH_STEP_EXECUTION (
                STEP_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
                VERSION BIGINT NOT NULL,
                STEP_NAME VARCHAR(100) NOT NULL,
                JOB_EXECUTION_ID BIGINT NOT NULL,
                CREATE_TIME TIMESTAMP NOT NULL,
                START_TIME TIMESTAMP,
                END_TIME TIMESTAMP,
                STATUS VARCHAR(10),
                COMMIT_COUNT BIGINT,
                READ_COUNT BIGINT,
                FILTER_COUNT BIGINT,
                WRITE_COUNT BIGINT,
                READ_SKIP_COUNT BIGINT,
                WRITE_SKIP_COUNT BIGINT,
                PROCESS_SKIP_COUNT BIGINT,
                ROLLBACK_COUNT BIGINT,
                EXIT_CODE VARCHAR(2500),
                EXIT_MESSAGE VARCHAR(2500),
                LAST_UPDATED TIMESTAMP
            );
        """);
    }

    @Test
    @DisplayName("BatchJobExecutor가 주입되면 직접 실행기에 위임하여 배치를 구동한다")
    void triggerBatch_withExecutor_delegatesDirectly() {
        BatchJobExecutor mockExecutor = (jobName, params) ->
                new BatchTriggerResponse(jobName, "EXEC-100", "STARTED", "Custom started");

        ExternalBatchTriggerAdapter adapterWithExecutor = new ExternalBatchTriggerAdapter(jdbcTemplate, mockExecutor);

        BatchTriggerResponse response = adapterWithExecutor.triggerBatch("allowanceEclJob", Map.of("baseDate", "2026-04-15"));

        assertThat(response.getJobName()).isEqualTo("allowanceEclJob");
        assertThat(response.getExecutionId()).isEqualTo("EXEC-100");
        assertThat(response.getStatus()).isEqualTo("STARTED");
        assertThat(response.getMessage()).isEqualTo("Custom started");
    }

    @Test
    @DisplayName("직접 실행기가 없을 때 triggerBatch는 트리거 기록을 DB 및 메모리에 보관하고 STARTED를 반환한다")
    void triggerBatch_withoutExecutor_persistsRecordAndReturnsStarted() {
        Map<String, Object> params = Map.of("baseDate", "2026-04-15", "eventId", "EVT-999");
        BatchTriggerResponse response = adapter.triggerBatch("allowanceEclJob", params);

        assertThat(response.getJobName()).isEqualTo("allowanceEclJob");
        assertThat(response.getExecutionId()).startsWith("EXT-");
        assertThat(response.getStatus()).isEqualTo("STARTED");

        // Fallback status verifies trigger record in DB/memory
        BatchStatusResponse status = adapter.getBatchStatus("allowanceEclJob");
        assertThat(status.getStatus()).isEqualTo("STARTED");
        assertThat(status.getSteps()).isNotEmpty();
        assertThat(status.getSteps().get(0).getName()).isEqualTo("allowanceExposureSyncStep");
        assertThat(status.getSteps().get(0).getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("이미 완료된 eventId로 triggerBatch 호출 시 BatchAlreadyCompletedException 예외를 던진다")
    void triggerBatch_withCompletedEvent_throwsBatchAlreadyCompletedException() {
        createSpringBatchMetadataTables();

        jdbcTemplate.update("INSERT INTO BATCH_JOB_INSTANCE VALUES (1, 1, 'allowanceEclJob', 'key1')");
        jdbcTemplate.update("INSERT INTO BATCH_JOB_EXECUTION (JOB_EXECUTION_ID, VERSION, JOB_INSTANCE_ID, CREATE_TIME, STATUS) VALUES (10, 1, 1, CURRENT_TIMESTAMP, 'COMPLETED')");
        jdbcTemplate.update("INSERT INTO BATCH_JOB_EXECUTION_PARAMS VALUES (10, 'eventId', 'STRING', 'EVT-ALREADY-DONE', 'Y')");

        assertThatThrownBy(() -> adapter.triggerBatch("allowanceEclJob", Map.of("eventId", "EVT-ALREADY-DONE")))
                .isInstanceOf(BatchAlreadyCompletedException.class)
                .hasMessageContaining("EVT-ALREADY-DONE");
    }

    @Test
    @DisplayName("Spring Batch 메타데이터 테이블에서 완료된 작업의 상태 및 100% 진척도를 정확히 조회한다")
    void getBatchStatus_withCompletedSpringBatchMetadata_returnsAccurateStatus() {
        createSpringBatchMetadataTables();

        Timestamp startTime = Timestamp.valueOf(LocalDateTime.of(2026, 4, 15, 10, 0, 0));
        Timestamp endTime = Timestamp.valueOf(LocalDateTime.of(2026, 4, 15, 10, 5, 0));

        jdbcTemplate.update("INSERT INTO BATCH_JOB_INSTANCE VALUES (1, 1, 'allowanceEclJob', 'key1')");
        jdbcTemplate.update("INSERT INTO BATCH_JOB_EXECUTION (JOB_EXECUTION_ID, VERSION, JOB_INSTANCE_ID, CREATE_TIME, START_TIME, END_TIME, STATUS, EXIT_CODE) " +
                "VALUES (10, 1, 1, ?, ?, ?, 'COMPLETED', 'COMPLETED')", startTime, startTime, endTime);

        String[] stepNames = {
                "allowanceExposureSyncStep", "dqStep", "stagingWarmingStep", "resultPreparationStep",
                "stagingManagerStep", "exposureWarmingStep", "eadCrmManagerStep", "reportingWarmingStep",
                "eclManagerStep", "allowanceEclCompletionStep", "allowanceSummaryStep"
        };

        for (int i = 0; i < stepNames.length; i++) {
            jdbcTemplate.update("INSERT INTO BATCH_STEP_EXECUTION (STEP_EXECUTION_ID, VERSION, STEP_NAME, JOB_EXECUTION_ID, CREATE_TIME, START_TIME, END_TIME, STATUS, EXIT_CODE, READ_COUNT, WRITE_COUNT) " +
                    "VALUES (?, 1, ?, 10, ?, ?, ?, 'COMPLETED', 'COMPLETED', 100, 100)", i + 1, stepNames[i], startTime, startTime, endTime);
        }

        BatchStatusResponse status = adapter.getBatchStatus("allowanceEclJob");

        assertThat(status.getJobId()).isEqualTo(10L);
        assertThat(status.getStatus()).isEqualTo("COMPLETED");
        assertThat(status.getStartTime()).isEqualTo(startTime.toLocalDateTime());
        assertThat(status.getEndTime()).isEqualTo(endTime.toLocalDateTime());
        assertThat(status.getProgress()).isEqualTo(100.0);
        assertThat(status.getSteps()).hasSize(11);
        assertThat(status.getSteps())
                .extracting(BatchStatusResponse.StepStatusDto::getStatus)
                .containsOnly("COMPLETED");
        assertThat(status.getSteps().get(0).getReadCount()).isEqualTo(100L);
        assertThat(status.getSteps().get(0).getWriteCount()).isEqualTo(100L);
    }

    @Test
    @DisplayName("진행 중인 스텝이 있는 경우 진척도(Progress)를 정확히 비례 계산한다")
    void getBatchStatus_withPartialStepExecution_calculatesPartialProgress() {
        createSpringBatchMetadataTables();

        Timestamp startTime = Timestamp.valueOf(LocalDateTime.of(2026, 4, 15, 10, 0, 0));

        jdbcTemplate.update("INSERT INTO BATCH_JOB_INSTANCE VALUES (1, 1, 'allowanceEclJob', 'key1')");
        jdbcTemplate.update("INSERT INTO BATCH_JOB_EXECUTION (JOB_EXECUTION_ID, VERSION, JOB_INSTANCE_ID, CREATE_TIME, START_TIME, STATUS) " +
                "VALUES (10, 1, 1, ?, ?, 'STARTED')", startTime, startTime);

        // 2 completed steps, 1 running step out of 11 standard steps
        jdbcTemplate.update("INSERT INTO BATCH_STEP_EXECUTION (STEP_EXECUTION_ID, VERSION, STEP_NAME, JOB_EXECUTION_ID, CREATE_TIME, STATUS, EXIT_CODE, READ_COUNT, WRITE_COUNT) " +
                "VALUES (1, 1, 'allowanceExposureSyncStep', 10, ?, 'COMPLETED', 'COMPLETED', 50, 50)", startTime);
        jdbcTemplate.update("INSERT INTO BATCH_STEP_EXECUTION (STEP_EXECUTION_ID, VERSION, STEP_NAME, JOB_EXECUTION_ID, CREATE_TIME, STATUS, EXIT_CODE, READ_COUNT, WRITE_COUNT) " +
                "VALUES (2, 1, 'dqStep', 10, ?, 'COMPLETED', 'COMPLETED', 50, 50)", startTime);
        jdbcTemplate.update("INSERT INTO BATCH_STEP_EXECUTION (STEP_EXECUTION_ID, VERSION, STEP_NAME, JOB_EXECUTION_ID, CREATE_TIME, STATUS, EXIT_CODE, READ_COUNT, WRITE_COUNT) " +
                "VALUES (3, 1, 'stagingWarmingStep', 10, ?, 'STARTED', 'EXECUTING', 10, 0)", startTime);

        BatchStatusResponse status = adapter.getBatchStatus("allowanceEclJob");

        assertThat(status.getStatus()).isEqualTo("STARTED");
        assertThat(status.getSteps()).hasSize(11);
        assertThat(status.getSteps().get(0).getStatus()).isEqualTo("COMPLETED");
        assertThat(status.getSteps().get(1).getStatus()).isEqualTo("COMPLETED");
        assertThat(status.getSteps().get(2).getStatus()).isEqualTo("STARTED");
        assertThat(status.getSteps().get(3).getStatus()).isEqualTo("PENDING");

        // (2 * 100 + 1 * 50) / 11 = 250 / 11 ~= 22.7%
        assertThat(status.getProgress()).isGreaterThan(20.0).isLessThan(25.0);
    }

    @Test
    @DisplayName("아무런 배치 실행 이력이 없을 때 getBatchStatus는 empty 응답(NO_HISTORY, 0.0)을 반환한다")
    void getBatchStatus_noHistory_returnsEmpty() {
        createSpringBatchMetadataTables();

        BatchStatusResponse status = adapter.getBatchStatus("unknownJob");

        assertThat(status.getStatus()).isEqualTo("NO_HISTORY");
        assertThat(status.getProgress()).isEqualTo(0.0);
        assertThat(status.getSteps()).isEmpty();
    }
}
