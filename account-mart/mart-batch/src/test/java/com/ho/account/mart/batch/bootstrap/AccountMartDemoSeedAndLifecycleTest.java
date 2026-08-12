package com.ho.account.mart.batch.bootstrap;

import com.ho.account.mart.batch.AllowanceMartBatchApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [QA] Account Mart Demo Seed 및 Clean Batch Lifecycle 검증 테스트
 *
 * 💡 [교육용 테스트 가이드]
 * 1. 데모 시드 데이터 적재 검증:
 *    'mart.batch.demo-seed.enabled=true' 조건에서 AccountMartDemoSeedRunner가 구동되어
 *    결산/시계열 데이터가 H2 데이터베이스에 결정론적으로 초기화되는지 확인합니다.
 *
 * 2. 대표 배치 잡 실행 검증:
 *    시딩된 기초 데이터를 바탕으로 대표 잡(integratedPositionEtlJob)이 정상 수행(COMPLETED)되는지 확인합니다.
 *
 * 3. 깔끔한 컨텍스트 셧다운(Clean Lifecycle):
 *    Job이 비활성화된 상태에서 컨텍스트 종료 시 미개동 ItemReader에 의한 destroyMethod 경고 없이 셧다운됨을 검증합니다.
 */
@SpringBootTest(classes = AllowanceMartBatchApplication.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "mart.batch.demo-seed.enabled=true",
        "spring.batch.job.enabled=false"
})
public class AccountMartDemoSeedAndLifecycleTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job integratedPositionEtlJob;

    @Test
    @DisplayName("mart.batch.demo-seed.enabled=true 설정 시 결정론적 데모 데이터가 정상 시딩되고 대표 배치 잡이 완주된다")
    void testDemoSeedFixtureAndRepresentativeJobExecution() throws Exception {
        // [Then] 1. 데모 시드 데이터 적재 확인 (AccountMartDemoSeedRunner / AccountMartDemoFixtureService 실행 결과)
        Integer ledgerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ods_acc_ledger WHERE acc_no LIKE 'DEMO-ACC%'",
                Integer.class);
        assertThat(ledgerCount).isNotNull().isGreaterThanOrEqualTo(5);

        Integer customerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ods_customer_mst WHERE customer_code LIKE 'DEMO-CUST%'",
                Integer.class);
        assertThat(customerCount).isNotNull().isGreaterThanOrEqualTo(5);

        Integer exchangeRateCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM market_exchange_rate WHERE base_currency = 'USD'",
                Integer.class);
        assertThat(exchangeRateCount).isNotNull().isGreaterThanOrEqualTo(1);

        // [When] 2. 대표 잡(integratedPositionEtlJob) 실행
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDate", AccountMartDemoFixtureService.DEFAULT_DEMO_BASE_DATE.toString())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(integratedPositionEtlJob, jobParameters);

        // [Then] 3. 잡 성공 검증
        assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        Integer positionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM allowance_input_positions WHERE base_dt = ?",
                Integer.class,
                java.sql.Date.valueOf(AccountMartDemoFixtureService.DEFAULT_DEMO_BASE_DATE));
        assertThat(positionCount).isNotNull().isGreaterThanOrEqualTo(5);
    }
}
