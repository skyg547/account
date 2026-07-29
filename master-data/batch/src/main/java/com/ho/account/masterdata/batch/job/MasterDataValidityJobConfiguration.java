package com.ho.account.masterdata.batch.job;

import com.ho.account.masterdata.batch.application.MasterDataBatchOrchestrator;
import com.ho.account.masterdata.batch.application.MasterDataBatchReport;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Wires the daily Master Data validity report as an executable Spring Batch
 * job.
 *
 * <p>This class owns only job/step flow and parameter mapping. The actual
 * validity rules and database aggregation stay in the core pipeline. That
 * separation prevents a scheduler-specific class from becoming the second
 * implementation of the business rule.</p>
 */
@Configuration
public class MasterDataValidityJobConfiguration {

    public static final String JOB_NAME = "masterDataValidityJob";
    public static final String STEP_NAME = "masterDataValidityStep";
    public static final String AS_OF_DATE_PARAMETER = "asOfDate";

    /**
     * Creates a restartable job identified by Spring Batch job parameters.
     * Operators must pass {@code asOfDate=yyyy-MM-dd}; silently substituting
     * today's date would make a restarted accounting report non-deterministic.
     */
    @Bean
    public Job masterDataValidityJob(JobRepository jobRepository, Step masterDataValidityStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(masterDataValidityStep)
                .build();
    }

    /**
     * Runs one bounded aggregate step. The core adapter executes database
     * COUNT queries, so a chunk of entity rows is neither loaded nor retained
     * in memory for this summary-style job.
     */
    @Bean
    public Step masterDataValidityStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            org.springframework.batch.core.step.tasklet.Tasklet masterDataValidityTasklet) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .tasklet(masterDataValidityTasklet, transactionManager)
                .build();
    }

    /**
     * Late binding keeps the job definition loadable when no job is launched,
     * while still failing before any business query when the identifying date
     * is missing or malformed.
     */
    @Bean
    @StepScope
    public org.springframework.batch.core.step.tasklet.Tasklet masterDataValidityTasklet(
            MasterDataBatchOrchestrator orchestrator,
            @Value("#{jobParameters['asOfDate']}") String asOfDateParameter) {
        return (contribution, chunkContext) -> {
            LocalDate asOfDate = requireAsOfDate(asOfDateParameter);
            MasterDataBatchReport report = orchestrator.createDailyValidityReport(asOfDate);

            // Primitive values are stored in the execution context so restart
            // metadata does not depend on Java record serialization details.
            ExecutionContext context = contribution.getStepExecution().getExecutionContext();
            context.putString(AS_OF_DATE_PARAMETER, report.asOfDate().toString());
            context.putLong("activeAccountSubjects", report.activeAccountSubjects());
            context.putLong("activeDepartments", report.activeDepartments());
            context.putLong("activeProducts", report.activeProducts());
            context.putLong("activeBusinessPartners", report.activeBusinessPartners());
            return RepeatStatus.FINISHED;
        };
    }

    static LocalDate requireAsOfDate(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException(
                    "Job parameter 'asOfDate' is required in yyyy-MM-dd format");
        }
        try {
            return LocalDate.parse(rawValue);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Job parameter 'asOfDate' must use yyyy-MM-dd format", exception);
        }
    }
}
