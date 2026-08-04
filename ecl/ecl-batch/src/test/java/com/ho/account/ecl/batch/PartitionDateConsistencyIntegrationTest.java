package com.ho.account.ecl.batch;

import com.ho.account.ecl.core.domain.result.AllowanceEclResult;
import com.ho.account.shared.finance.enums.CalculationStatus;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [회귀 테스트] 파티셔닝 기준일(baseDate) 정합성 및 멀티 기준일 계좌/결과 격리 검증 테스트.
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
public class PartitionDateConsistencyIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    @Qualifier("exposureLgdJob")
    private Job exposureLgdJob;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(exposureLgdJob);
        setupTestData();
    }

    private void setupTestData() {
        createBatchMetadataTables();
        createAllowanceTables();

        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.update("TRUNCATE TABLE allowance_ecl_results");
        jdbcTemplate.update("DELETE FROM cr_accounts");
        jdbcTemplate.update("DELETE FROM cr_customers");
        jdbcTemplate.update("DELETE FROM cr_product_masters");
        jdbcTemplate.update("DELETE FROM cr_grade_masters");
        jdbcTemplate.update("DELETE FROM cr_lgd_segment_masters");
        jdbcTemplate.update("DELETE FROM cr_macro_scenario");
        jdbcTemplate.update("DELETE FROM allowance_model_parameters");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        // 1. cr_accounts 삽입 (disjoint IDs: 101, 102)
        jdbcTemplate.update("INSERT INTO cr_customers (id, customer_code, customer_name, customer_type, is_active) VALUES (1, 'C1', 'Cust1', 'CORPORATE', TRUE)");
        jdbcTemplate.update("""
                INSERT INTO cr_accounts (id, account_no, customer_id, product_code, currency, notional_amt, outstanding_amt, open_date, maturity_date, delinquent_days, is_active)
                VALUES (101, 'ACC-101', 1, 'LN-1', 'KRW', 1000000.00, 1000000.00, DATE '2025-01-01', DATE '2027-01-01', 0, TRUE),
                       (102, 'ACC-102', 1, 'LN-1', 'KRW', 2000000.00, 2000000.00, DATE '2025-01-01', DATE '2027-01-01', 0, TRUE)
                """);

        jdbcTemplate.update("INSERT INTO cr_product_masters (product_code, product_name, ccf_rate) VALUES ('LN-1', '대출', 0.5)");
        jdbcTemplate.update("INSERT INTO cr_grade_masters (rating_code, pd_value, notch_order) VALUES ('A', 0.01000000, 1)");
        jdbcTemplate.update("INSERT INTO allowance_model_parameters (param_key, param_value) VALUES ('PD_FLOOR', 0.0005), ('SECURED_LGD_FLOOR', 0.20), ('UNSECURED_LGD_FLOOR', 0.45), ('DEFAULT_DISCOUNT_RATE', 0.05)");
        jdbcTemplate.update("INSERT INTO cr_lgd_segment_masters (segment_name, customer_type, collateral_type, lgd_value) VALUES ('기업_무담보', 'CORPORATE', 'UNSECURED', 0.45)");
        jdbcTemplate.update("INSERT INTO cr_macro_scenario (scenario_type, apply_year, pd_adjustment_factor, probability_weight) VALUES ('BOOM', 2026, 0.80, 0.20), ('BASE', 2026, 1.00, 0.60), ('RECESSION', 2026, 1.30, 0.20)");

        // 2. allowance_ecl_results 2개 기준일 데이터 삽입 (disjoint result IDs: 1~5 for 2026-04-15, 6~10 for 2026-04-30)
        // 2026-04-15 결과들
        jdbcTemplate.update("""
                INSERT INTO allowance_ecl_results (id, base_date, account_id, staging, pd, status)
                VALUES (1, DATE '2026-04-15', 101, 'STAGE1', 0.01000000, 'RUNNING'),
                       (2, DATE '2026-04-15', 102, 'STAGE1', 0.01000000, 'RUNNING')
                """);

        // 2026-04-30 결과들
        jdbcTemplate.update("""
                INSERT INTO allowance_ecl_results (id, base_date, account_id, staging, pd, status)
                VALUES (6, DATE '2026-04-30', 101, 'STAGE1', 0.02000000, 'RUNNING'),
                       (7, DATE '2026-04-30', 102, 'STAGE1', 0.02000000, 'RUNNING')
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
                CREATE TABLE IF NOT EXISTS allowance_ecl_results (
                    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    base_date DATE NOT NULL,
                    account_id BIGINT NOT NULL,
                    staging VARCHAR(20),
                    pd NUMERIC(10,8),
                    ead NUMERIC(19,4),
                    ead_star NUMERIC(19,4),
                    lgd NUMERIC(10,8),
                    status VARCHAR(20)
                )
                """);
    }

    @Test
    @DisplayName("exposureLgdJob 실행 시 지정된 baseDate의 파티션만 정확히 처리하고 다른 기준일 데이터는 격리된다")
    void testPartitionBaseDateIsolation() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDate", "2026-04-15")
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // 2026-04-15 항목들만 EAD/LGD 처리 완료(status='COMPLETED' 또는 ead IS NOT NULL)되어야 함
        Integer date1UpdatedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM allowance_ecl_results WHERE base_date = '2026-04-15' AND ead IS NOT NULL",
                Integer.class);
        assertThat(date1UpdatedCount).isEqualTo(2);

        // 2026-04-30 항목들은 EAD 처리 없이 미변경(status='RUNNING', ead IS NULL)이어야 함
        Integer date2UntouchedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM allowance_ecl_results WHERE base_date = '2026-04-30' AND ead IS NULL",
                Integer.class);
        assertThat(date2UntouchedCount).isEqualTo(2);
    }

    @Test
    @DisplayName("데이터가 존재하지 않는 빈 기준일(empty date) 파티셔닝도 예외 없이 안전하게 완주한다")
    void testEmptyDatePartitioning() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDate", "2026-05-01")
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}
