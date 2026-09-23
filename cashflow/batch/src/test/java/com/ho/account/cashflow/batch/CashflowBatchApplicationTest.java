package com.ho.account.cashflow.batch;

import com.ho.account.cashflow.core.application.port.in.CashflowStatementUseCase;
import com.ho.account.cashflow.core.application.port.out.LedgerCashBalancePort;
import com.ho.account.cashflow.core.infrastructure.memory.InMemoryLedgerCashBalanceAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CashflowBatchApplicationTest {

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private Job cashflowAggregationJob;

    @Autowired
    private CashflowStatementUseCase statementUseCase;

    @Autowired
    private LedgerCashBalancePort ledgerCashBalancePort;

    @Autowired
    private JobLauncher jobLauncher;

    @Test
    void localProfileBootsWithBatchInfrastructureJobAndCoreWiring() {
        assertThat(context.isActive()).isTrue();
        assertThat(jobExplorer).isNotNull();
        assertThat(cashflowAggregationJob.getName()).isEqualTo(CashflowAggregationJobConfig.JOB_NAME);
        assertThat(statementUseCase).isNotNull();
        assertThat(ledgerCashBalancePort).isInstanceOf(InMemoryLedgerCashBalanceAdapter.class);
    }

    @Test
    void completesOnceAndRejectsTheSameCompletedInstanceInOneRepositoryLifetime() throws Exception {
        JobParameters parameters = completeParameters("stmt-batch-repeat");

        JobExecution firstExecution = jobLauncher.run(cashflowAggregationJob, parameters);

        assertThat(firstExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThatThrownBy(() -> jobLauncher.run(cashflowAggregationJob, parameters))
                .isInstanceOf(JobInstanceAlreadyCompleteException.class);
    }

    @Test
    void marksTheJobFailedWhenARequiredParameterIsMissing() throws Exception {
        JobParameters parameters = new JobParametersBuilder()
                .addLong("fiscalYear", 2026L)
                .addLong("fiscalPeriod", 10L)
                .addString("method", "DIRECT")
                .addString("currency", "KRW")
                .addString("generatedAt", "2026-09-23T12:00:00")
                .toJobParameters();

        JobExecution execution = jobLauncher.run(cashflowAggregationJob, parameters);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(execution.getAllFailureExceptions())
                .anySatisfy(exception -> assertThat(exception).hasMessageContaining("statementId"));
    }

    private static JobParameters completeParameters(String statementId) {
        return new JobParametersBuilder()
                .addString("statementId", statementId)
                .addLong("fiscalYear", 2026L)
                .addLong("fiscalPeriod", 9L)
                .addString("method", "DIRECT")
                .addString("currency", "KRW")
                .addString("generatedAt", "2026-09-23T11:00:00")
                .toJobParameters();
    }
}
