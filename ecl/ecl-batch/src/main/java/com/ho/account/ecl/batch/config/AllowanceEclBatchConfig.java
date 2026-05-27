package com.ho.account.ecl.batch.config;

import com.ho.account.ecl.batch.job.tasklet.AllowanceEclCompletionTasklet;
import com.ho.account.ecl.batch.job.tasklet.AllowanceExposureSyncTasklet;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class AllowanceEclBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final AllowanceExposureSyncTasklet allowanceExposureSyncTasklet;
    private final AllowanceEclCompletionTasklet allowanceEclCompletionTasklet;

    @Qualifier("dqStep")
    private final Step dqStep;
    @Qualifier("stagingWarmingStep")
    private final Step stagingWarmingStep;
    @Qualifier("resultPreparationStep")
    private final Step resultPreparationStep;
    @Qualifier("stagingManagerStep")
    private final Step stagingManagerStep;
    @Qualifier("exposureWarmingStep")
    private final Step exposureWarmingStep;
    @Qualifier("eadCrmManagerStep")
    private final Step eadCrmManagerStep;
    @Qualifier("reportingWarmingStep")
    private final Step reportingWarmingStep;
    @Qualifier("eclManagerStep")
    private final Step eclManagerStep;
    @Qualifier("allowanceSummaryStep")
    private final Step allowanceSummaryStep;

    @Bean
    public Job allowanceEclJob() {
        return new JobBuilder("allowanceEclJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(allowanceExposureSyncStep())
                .next(dqStep)
                .next(stagingWarmingStep)
                .next(resultPreparationStep)
                .next(stagingManagerStep)
                .next(exposureWarmingStep)
                .next(eadCrmManagerStep)
                .next(reportingWarmingStep)
                .next(eclManagerStep)
                .next(allowanceEclCompletionStep())
                .next(allowanceSummaryStep)
                .build();
    }

    @Bean
    public Step allowanceEclCompletionStep() {
        return new StepBuilder("allowanceEclCompletionStep", jobRepository)
                .tasklet(allowanceEclCompletionTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step allowanceExposureSyncStep() {
        return new StepBuilder("allowanceExposureSyncStep", jobRepository)
                .tasklet(allowanceExposureSyncTasklet, transactionManager)
                .build();
    }
}
