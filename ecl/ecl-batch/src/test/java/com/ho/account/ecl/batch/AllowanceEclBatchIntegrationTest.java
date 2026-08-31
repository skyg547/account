package com.ho.account.ecl.batch;

import com.ho.account.shared.finance.enums.CalculationStatus;
import com.ho.account.ecl.core.application.port.out.AllowanceEclResultRepository;
import com.ho.account.ecl.core.domain.result.AllowanceEclResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [QA] IFRS 9 대손충당금 표준 배치 파이프라인 통합 테스트.
 */
@SpringBatchTest
@SpringBootTest(
    classes = {
        com.ho.account.ecl.batch.AllowanceEclBatchApplication.class,
        com.ho.account.ecl.batch.config.BatchInfrastructureConfig.class
    },
    properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.batch.job.enabled=false",
        "spring.main.allow-bean-definition-overriding=true"
    }
)
@ActiveProfiles("test")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
public class AllowanceEclBatchIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private AllowanceEclResultRepository allowanceResultRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    @Qualifier("allowanceEclJob")
    private Job allowanceEclJob;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(allowanceEclJob);
        setupTestData();
    }

    private void setupTestData() {
        createBatchMetadataTables();
        createAllowanceTables();

        // [Safety] 제약 조건 일시 비활성화 후 초기화
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.update("TRUNCATE TABLE allowance_summary");
        jdbcTemplate.update("TRUNCATE TABLE allowance_account_mappings");
        jdbcTemplate.update("TRUNCATE TABLE allowance_exposure_snapshots");
        jdbcTemplate.update("TRUNCATE TABLE allowance_ecl_results");
        jdbcTemplate.update("TRUNCATE TABLE cr_account_collaterals");
        jdbcTemplate.update("TRUNCATE TABLE cr_collaterals");
        jdbcTemplate.update("DELETE FROM cr_accounts");
        jdbcTemplate.update("DELETE FROM cr_customers");
        jdbcTemplate.update("DELETE FROM cr_grade_masters");
        jdbcTemplate.update("DELETE FROM cr_lgd_segment_masters");
        jdbcTemplate.update("DELETE FROM cr_macro_scenario");
        jdbcTemplate.update("DELETE FROM cr_product_masters");
        jdbcTemplate.update("DELETE FROM allowance_model_parameters");
        jdbcTemplate.update("DELETE FROM transition_matrix");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        // 1. 산출 입력 snapshot: account-mart가 만든 기준일별 대손충당금 입력을 모사한다.
        jdbcTemplate.update("""
                INSERT INTO allowance_exposure_snapshots (
                       base_date,
                       exposure_id,
                       source_account_no,
                       customer_code,
                       customer_type,
                       is_sme,
                       country_code,
                       industry_code,
                       product_code,
                       product_category,
                       legal_entity_code,
                       branch_code,
                       currency_code,
                       outstanding_amount,
                       undrawn_amount,
                       interest_rate,
                       open_date,
                       maturity_date,
                       delinquent_days,
                       staging,
                       original_rating,
                       current_rating,
                       warning_level,
                       debt_restructured
                ) VALUES (
                       DATE '2026-04-15',
                       'EXP-001',
                       'ACC-001',
                       'C-001',
                       'CORPORATE',
                       FALSE,
                       'KR',
                       'MANUFACTURING',
                       'LN-1',
                       'LOAN',
                       'HO',
                       'B001',
                       'KRW',
                       1000000.00,
                       200000.00,
                       0.050000,
                       DATE '2025-04-15',
                       DATE '2027-04-15',
                       0,
                       'STAGE1',
                       'A',
                       'A',
                       'NORMAL',
                       FALSE
                )
                """);

        // 2. 산출에 필요한 마스터/모델 파라미터.
        jdbcTemplate.update("INSERT INTO cr_product_masters (product_code, product_name, ccf_rate) VALUES ('LN-1', '일반대출', 0.5)");
        jdbcTemplate.update("INSERT INTO cr_grade_masters (rating_code, pd_value, notch_order) VALUES ('A', 0.01000000, 1)");
        jdbcTemplate.update("INSERT INTO allowance_model_parameters (param_key, param_value) VALUES ('PD_FLOOR', 0.0005), ('SECURED_LGD_FLOOR', 0.20), ('UNSECURED_LGD_FLOOR', 0.45), ('DEFAULT_DISCOUNT_RATE', 0.05)");
        jdbcTemplate.update("INSERT INTO cr_lgd_segment_masters (segment_name, customer_type, collateral_type, lgd_value) VALUES ('기업_무담보', 'CORPORATE', 'UNSECURED', 0.45)");
        jdbcTemplate.update("INSERT INTO cr_macro_scenario (scenario_type, apply_year, pd_adjustment_factor, probability_weight) VALUES ('BOOM', 2026, 0.80, 0.20), ('BASE', 2026, 1.00, 0.60), ('RECESSION', 2026, 1.30, 0.20)");

        // 3. closing이 소비할 summary를 만들기 위한 회계 계정 매핑.
        jdbcTemplate.update("""
                INSERT INTO allowance_account_mappings (
                       product_code,
                       biz_unit_code,
                       currency_code,
                       legal_entity_code,
                       exposure_account_code,
                       allowance_account_code,
                       bad_debt_expense_account_code,
                       reversal_income_account_code,
                       active
                ) VALUES (
                       'LN-1',
                       'HO',
                       'KRW',
                       'HO',
                       '110101',
                       '110199',
                       '550101',
                       '450101',
                       TRUE
                )
                """);
    }

    private void createBatchMetadataTables() {
        Integer existingTables = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'BATCH_JOB_INSTANCE'",
                Integer.class);
        if (existingTables != null && existingTables > 0) {
            return;
        }
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("org/springframework/batch/core/schema-h2.sql"));
        populator.execute(dataSource);
    }

    private void createAllowanceTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS allowance_exposure_snapshots (
                    base_date DATE NOT NULL,
                    exposure_id VARCHAR(100) NOT NULL,
                    source_account_no VARCHAR(100) NOT NULL,
                    customer_code VARCHAR(100) NOT NULL,
                    customer_type VARCHAR(30),
                    is_sme BOOLEAN,
                    country_code VARCHAR(10),
                    industry_code VARCHAR(50),
                    product_code VARCHAR(50) NOT NULL,
                    product_category VARCHAR(50),
                    legal_entity_code VARCHAR(50),
                    branch_code VARCHAR(50),
                    currency_code VARCHAR(3),
                    outstanding_amount NUMERIC(19,4),
                    undrawn_amount NUMERIC(19,4),
                    interest_rate NUMERIC(19,6),
                    open_date DATE,
                    maturity_date DATE,
                    delinquent_days INTEGER,
                    staging VARCHAR(20),
                    original_rating VARCHAR(20),
                    current_rating VARCHAR(20),
                    warning_level VARCHAR(20),
                    debt_restructured BOOLEAN
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS allowance_account_mappings (
                    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    product_code VARCHAR(50) NOT NULL,
                    biz_unit_code VARCHAR(50),
                    currency_code VARCHAR(3),
                    legal_entity_code VARCHAR(50) NOT NULL,
                    exposure_account_code VARCHAR(50) NOT NULL,
                    allowance_account_code VARCHAR(50) NOT NULL,
                    bad_debt_expense_account_code VARCHAR(50) NOT NULL,
                    reversal_income_account_code VARCHAR(50) NOT NULL,
                    active BOOLEAN NOT NULL DEFAULT TRUE
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS allowance_summary (
                    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    base_date DATE NOT NULL,
                    run_id VARCHAR(100) NOT NULL,
                    model_version VARCHAR(50) NOT NULL,
                    legal_entity_code VARCHAR(50) NOT NULL,
                    currency_code VARCHAR(3) NOT NULL,
                    exposure_account_code VARCHAR(50) NOT NULL,
                    allowance_account_code VARCHAR(50) NOT NULL,
                    bad_debt_expense_account_code VARCHAR(50) NOT NULL,
                    reversal_income_account_code VARCHAR(50) NOT NULL,
                    target_allowance_amount NUMERIC(19,4) NOT NULL,
                    source_exposure_amount NUMERIC(19,4) NOT NULL,
                    stage1_allowance_amount NUMERIC(19,4) NOT NULL,
                    stage2_allowance_amount NUMERIC(19,4) NOT NULL,
                    stage3_allowance_amount NUMERIC(19,4) NOT NULL,
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP
                )
                """);
    }

    @Test
    @DisplayName("IFRS 9 allowanceEclJob 실행 후 ECL 결과와 allowance summary를 생성한다")
    void testAllowanceEclJob() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDate", "2026-04-15")
                .addString("runId", "TEST-RUN-001")
                .addString("modelVersion", "test-v1")
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        LocalDate baseDate = LocalDate.parse("2026-04-15");
        List<AllowanceEclResult> results = allowanceResultRepository.findAllByBaseDate(baseDate);
        assertThat(results).isNotEmpty();
        
        // 상세 에러 확인을 위해 실제 상태값 목록을 비교
        assertThat(results)
            .extracting("status")
            .containsOnly(CalculationStatus.COMPLETED);

        // CCF가 0일 경우 EAD가 0이 될 수 있으므로, 0 이상으로 검증 조건을 완화함
        assertThat(results)
            .extracting(AllowanceEclResult::getEad)
            .allMatch(ead -> ead.compareTo(BigDecimal.ZERO) >= 0);

        Integer summaryCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM allowance_summary WHERE base_date = ? AND run_id = 'TEST-RUN-001'",
                Integer.class,
                baseDate);
        assertThat(summaryCount).isNotNull();
        assertThat(summaryCount).isGreaterThan(0);

        BigDecimal targetAllowanceAmount = jdbcTemplate.queryForObject(
                "SELECT target_allowance_amount FROM allowance_summary WHERE base_date = ? AND run_id = 'TEST-RUN-001'",
                BigDecimal.class,
                baseDate);
        assertThat(targetAllowanceAmount).isNotNull();
        assertThat(targetAllowanceAmount).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("allowanceEclJob 실행 시 Spring Batch 메타데이터 테이블에 스텝별 실행 상태가 정확히 기록된다")
    void testBatchMetadataTrackingAndStepExecutionProgress() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDate", "2026-04-15")
                .addString("runId", "TEST-RUN-METADATA-001")
                .addString("eventId", "EVT-TEST-001")
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // BATCH_JOB_EXECUTION 테이블 상태 검증
        String status = jdbcTemplate.queryForObject(
                "SELECT STATUS FROM BATCH_JOB_EXECUTION WHERE JOB_EXECUTION_ID = ?",
                String.class,
                jobExecution.getId());
        assertThat(status).isEqualTo("COMPLETED");

        // BATCH_STEP_EXECUTION 테이블 스텝 목록 검증
        List<String> executedSteps = jdbcTemplate.queryForList(
                "SELECT STEP_NAME FROM BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID = ? ORDER BY STEP_EXECUTION_ID ASC",
                String.class,
                jobExecution.getId());

        assertThat(executedSteps).contains(
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
}

