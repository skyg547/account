package com.ho.account.mart.batch.job.ods;

import com.ho.account.shared.finance.entity.IntegratedRiskPosition;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [QA] 통합 리스크 포지션 적재 배치(ETL) 통합 테스트.
 * ODS(원천) 데이터를 CDM(마트) 모델로 변환하여 적재하는 파이프라인을 검증합니다.
 */
@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
public class IntegratedPositionEtlJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setJob(org.springframework.batch.core.Job integratedPositionEtlJob) {
        this.jobLauncherTestUtils.setJob(integratedPositionEtlJob);
    }

    @Test
    @DisplayName("demo 원천 데이터를 생성한 뒤 통합 포지션 배치가 완주되는지 테스트")
    public void testIntegratedPositionEtlJob() throws Exception {
        LocalDate baseDate = LocalDate.of(2026, 4, 15);
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDate", baseDate.toString())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        List<IntegratedRiskPosition> results = entityManager.createQuery(
                "SELECT p FROM IntegratedRiskPosition p WHERE p.baseDt = :baseDate ORDER BY p.accNo",
                IntegratedRiskPosition.class)
                .setParameter("baseDate", baseDate)
                .getResultList();

        assertThat(results).hasSize(5);
        assertThat(results).extracting(IntegratedRiskPosition::getAccNo)
                .containsExactly("DEMO-ACC001", "DEMO-ACC002", "DEMO-ACC003", "DEMO-ACC004", "DEMO-ACC005");

        IntegratedRiskPosition defaultedCard = results.stream()
                .filter(position -> "DEMO-ACC004".equals(position.getAccNo()))
                .findFirst()
                .orElseThrow();
        assertThat(defaultedCard.getStaging().name()).isEqualTo("STAGE3");

        IntegratedRiskPosition usdExposure = results.stream()
                .filter(position -> "DEMO-ACC005".equals(position.getAccNo()))
                .findFirst()
                .orElseThrow();
        assertThat(usdExposure.getMarketValue()).isEqualByComparingTo(new BigDecimal("13500000.0000"));

        Integer reconcileMatches = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ods_reconcile_hist WHERE base_dt = ? AND status = '정상(MATCH)'",
                Integer.class,
                java.sql.Date.valueOf(baseDate));
        assertThat(reconcileMatches).isEqualTo(4);

        Integer snapshotRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM allowance_exposure_snapshots WHERE base_date = ?",
                Integer.class,
                java.sql.Date.valueOf(baseDate));
        assertThat(snapshotRows).isEqualTo(5);

        BigDecimal undrawnAmount = jdbcTemplate.queryForObject(
                "SELECT undrawn_amount FROM allowance_exposure_snapshots WHERE base_date = ? AND exposure_id = ?",
                BigDecimal.class,
                java.sql.Date.valueOf(baseDate),
                "DEMO-ACC003");
        assertThat(undrawnAmount).isEqualByComparingTo(new BigDecimal("500000000.0000"));

        String accountingAccountCode = jdbcTemplate.queryForObject(
                "SELECT accounting_account_code FROM allowance_exposure_snapshots WHERE base_date = ? AND exposure_id = ?",
                String.class,
                java.sql.Date.valueOf(baseDate),
                "DEMO-ACC001");
        assertThat(accountingAccountCode).isEqualTo("L001");
    }
}
