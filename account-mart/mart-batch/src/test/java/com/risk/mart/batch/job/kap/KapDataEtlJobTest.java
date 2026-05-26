package com.risk.mart.batch.job.kap;

import com.risk.mart.core.domain.external.kap.entity.KapExternalRating;
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
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * [QA] KAP 대외 데이터 수집 배치(ETL) 통합 테스트.
 * CSV 데이터를 시스템 내부의 KAP 마스터 테이블로 정확히 적재하는지 검증합니다.
 */
@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
public class KapDataEtlJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    public void setJob(org.springframework.batch.core.Job kapDataEtlJob) {
        this.jobLauncherTestUtils.setJob(kapDataEtlJob);
    }

    @Test
    @DisplayName("KAP 외부 신용등급 CSV 파일이 DB 테이블에 정상적으로 적재되는지 테스트")
    public void testKapDataEtlJob() throws Exception {
        // [Given] 0. 기존 데이터 초기화
        jdbcTemplate.execute("DELETE FROM kap_external_rating");

        // [Given] 1. 테스트용 CSV 파일 경로 및 중복 실행 방지를 위한 랜덤 파라미터 설정
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("filePath", "data/kap/external_ratings.csv")
                .addLong("time", System.currentTimeMillis()) // 고유 파라미터 추가
                .toJobParameters();

        // [When] 배치 잡(Job) 실행
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // [Then] 결과 검증
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        // DB 조회 검증
        List<KapExternalRating> results = entityManager.createQuery(
                "SELECT r FROM KapExternalRating r", KapExternalRating.class)
                .getResultList();

        assertThat(results).hasSize(3);
        assertThat(results)
                .extracting(KapExternalRating::getCustomerId, KapExternalRating::getRatingGrade)
                .containsExactlyInAnyOrder(
                        tuple("C-001", "AAA"),
                        tuple("C-002", "AA+"),
                        tuple("C-003", "BBB"));

        KapExternalRating first = results.get(0);
        assertThat(first.getEvalAgency()).isEqualTo("KAP");
    }
}
