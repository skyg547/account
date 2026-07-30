package com.ho.account.budget.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.budget.application.port.in.BudgetYearEndUseCase;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

/**
 * Job을 실제 Spring Batch 인프라로 실행해 파라미터 검증, 위임, 처리 건수 관측을 함께 고정한다.
 */
@SpringBatchTest
@SpringBootTest(
        classes = BudgetYearEndCloseJobTest.TestApplication.class,
        properties = {
                "spring.batch.job.enabled=false",
                "spring.batch.jdbc.initialize-schema=always",
                "spring.datasource.url=jdbc:h2:mem:budget_year_end_job;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.flyway.enabled=false",
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false",
                "management.health.vault.enabled=false"
        })
class BudgetYearEndCloseJobTest {

    @MockBean
    private BudgetYearEndUseCase budgetYearEndUseCase;

    @jakarta.annotation.Resource
    private JobLauncherTestUtils jobLauncherTestUtils;

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidFiscalYears")
    void missingOrInvalidFiscalYearFailsBeforeCoreDelegation(String scenario, JobParameters parameters) {
        assertThatThrownBy(() -> jobLauncherTestUtils.launchJob(parameters))
                .isInstanceOf(JobParametersInvalidException.class)
                .hasMessageContaining("exactly four digits");
    }

    @Test
    void validFiscalYearDelegatesWithDefaultActorAndReportsClosedCount() throws Exception {
        when(budgetYearEndUseCase.closeFiscalYear("2026", "SYSTEM")).thenReturn(7);

        JobExecution execution = jobLauncherTestUtils.launchJob(
                new JobParametersBuilder()
                        .addString("fiscalYear", "2026", true)
                        .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions())
                .singleElement()
                .extracting(StepExecution::getWriteCount)
                .isEqualTo(7L);
        verify(budgetYearEndUseCase).closeFiscalYear("2026", "SYSTEM");
    }

    @Test
    void suppliedActorIsTrimmedAndDelegatedWithoutChangingBusinessRules() throws Exception {
        when(budgetYearEndUseCase.closeFiscalYear("2027", "operator")).thenReturn(1);

        JobExecution execution = jobLauncherTestUtils.launchJob(
                new JobParametersBuilder()
                        .addString("fiscalYear", "2027", true)
                        .addString("actor", " operator ", false)
                        .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        verify(budgetYearEndUseCase).closeFiscalYear("2027", "operator");
    }

    @Test
    void coreFailureFailsTheJobAndTheSameFiscalYearCanRestart() throws Exception {
        when(budgetYearEndUseCase.closeFiscalYear("2028", "SYSTEM"))
                .thenThrow(new IllegalStateException("core close failed"))
                .thenReturn(2);
        JobParameters parameters = new JobParametersBuilder()
                .addString("fiscalYear", "2028", true)
                .toJobParameters();

        JobExecution failed = jobLauncherTestUtils.launchJob(parameters);
        JobExecution restarted = jobLauncherTestUtils.launchJob(parameters);

        assertThat(failed.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(failed.getAllFailureExceptions())
                .anyMatch(exception -> exception.getMessage().contains("core close failed"));
        assertThat(restarted.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(restarted.getStepExecutions())
                .singleElement()
                .extracting(StepExecution::getWriteCount)
                .isEqualTo(2L);
        verify(budgetYearEndUseCase, times(2)).closeFiscalYear("2028", "SYSTEM");
    }

    @Test
    void completedFiscalYearCannotRunAgainWithoutAChangedIdentifyingParameter() throws Exception {
        when(budgetYearEndUseCase.closeFiscalYear("2029", "SYSTEM")).thenReturn(0);
        JobParameters parameters = new JobParametersBuilder()
                .addString("fiscalYear", "2029", true)
                .toJobParameters();

        assertThat(jobLauncherTestUtils.launchJob(parameters).getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThatThrownBy(() -> jobLauncherTestUtils.launchJob(parameters))
                .isInstanceOf(JobInstanceAlreadyCompleteException.class);
    }

    private static Stream<Arguments> invalidFiscalYears() {
        return Stream.of(
                Arguments.of("missing", new JobParameters()),
                Arguments.of(
                        "not four digits",
                        new JobParametersBuilder().addString("fiscalYear", "26").toJobParameters()),
                Arguments.of(
                        "contains non-digits",
                        new JobParametersBuilder().addString("fiscalYear", "20A6").toJobParameters()),
                Arguments.of(
                        "contains surrounding whitespace",
                        new JobParametersBuilder().addString("fiscalYear", " 2026 ").toJobParameters()));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(BudgetYearEndCloseJobConfiguration.class)
    static class TestApplication {
    }
}
