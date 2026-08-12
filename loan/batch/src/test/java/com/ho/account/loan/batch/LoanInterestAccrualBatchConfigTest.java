package com.ho.account.loan.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.ho.account.loan.application.pipeline.LoanInterestAccrualPipeline;

/**
 * [대출 이자 및 부대비용 상각 배치 Job 통합 및 구성 단위 테스트]
 *
 * <p><strong>Pedagogical Explanation & Design Intent / 교육적 주석:</strong></p>
 * <ul>
 *   <li><strong>Job Parameter Validation</strong>:
 *       배치 실행 시 필수 일자 파라미터인 {@code accrualDate}가 검증을 통과하는지 확인하여
 *       잘못된 인자로 배치가 수행되는 오작동을 차단합니다.</li>
 *   <li><strong>Self-Contained Local H2 Batch Execution</strong>:
 *       로컬 H2 환경에서 {@link JobLauncher}를 이용해 대표 대출 배치 Job({@code loanInterestAccrualJob})이
 *       성공적으로 기동되고 {@link ExitStatus#COMPLETED}로 종료되는 런타임 안정성을 검증합니다.</li>
 * </ul>
 */
@ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class LoanInterestAccrualBatchConfigTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job loanInterestAccrualJob;

    @Autowired
    private JobParametersValidator loanInterestAccrualJobParametersValidator;

    @MockBean
    private LoanInterestAccrualPipeline loanInterestAccrualPipeline;

    @Test
    @DisplayName("accrualDate 파라미터 없이 실행하려 하면 JobParametersInvalidException이 발생한다")
    void validateJobParameters_withoutAccrualDate_throwsException() {
        JobParameters emptyParameters = new JobParametersBuilder().toJobParameters();

        assertThatThrownBy(() -> loanInterestAccrualJobParametersValidator.validate(emptyParameters))
                .isInstanceOf(JobParametersInvalidException.class);
    }

    @Test
    @DisplayName("local H2 환경에서 accrualDate 파라미터와 함께 loanInterestAccrualJob 실행 시 COMPLETED 상태로 성공 종료된다")
    void runLoanInterestAccrualJob_success() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("accrualDate", "2026-05-21")
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(loanInterestAccrualJob, jobParameters);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    }
}
