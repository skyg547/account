package com.ho.account.receivable.batch;

import com.ho.account.receivable.application.port.in.ReceivableBatchUseCase;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ReceivableAutoMatchingBatchConfig {

    @Bean
    Job receivableAutoMatchingJob(JobRepository jobRepository, Step receivableAutoMatchingStep) {
        return new JobBuilder("receivableAutoMatchingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(receivableAutoMatchingStep)
                .build();
    }

    @Bean
    Step receivableAutoMatchingStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ReceivableBatchUseCase receivableBatchUseCase) {
        return new StepBuilder("receivableAutoMatchingStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    receivableBatchUseCase.runAutoMatching();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
