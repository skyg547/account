package com.ho.account.ecl.batch.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [단일 Step 개별 재수행용 Job 설정]
 * 
 * 💡 각 단계(Step)별로 재수행이 필요할 때 호출할 수 있도록 
 *    기존 Step들을 1개의 Job으로 감싸서(Wrap) 노출합니다.
 */
@Configuration
@RequiredArgsConstructor
public class IndividualStepJobConfig {

    private final JobRepository jobRepository;

    // --- Phase 0 ---
    @Qualifier("cdmSyncStep") private final Step cdmSyncStep;
    @Qualifier("allowanceExposureSyncStep") private final Step allowanceExposureSyncStep;

    // --- Phase 1 ---
    @Qualifier("dqStep") private final Step dqStep;

    // --- Phase 3 ---
    @Qualifier("resultPreparationStep") private final Step resultPreparationStep;
    @Qualifier("stagingManagerStep") private final Step stagingManagerStep;

    // --- Phase 4 ---
    @Qualifier("eadCrmManagerStep") private final Step eadCrmManagerStep;

    // --- Phase 5 ---
    @Qualifier("eclManagerStep") private final Step eclManagerStep;
    @Qualifier("allowanceEclCompletionStep") private final Step allowanceEclCompletionStep;
    @Qualifier("allowanceSummaryStep") private final Step allowanceSummaryStep;

    // --- Job 등록 ---
    @Bean public Job standaloneDqJob() { return wrapStep("standaloneDqJob", dqStep); }
    @Bean public Job standaloneResultPreparationJob() { return wrapStep("standaloneResultPreparationJob", resultPreparationStep); }
    @Bean public Job standaloneStagingJob() { return wrapStep("standaloneStagingJob", stagingManagerStep); }
    
    @Bean public Job standaloneEadCrmJob() { return wrapStep("standaloneEadCrmJob", eadCrmManagerStep); }
    
    @Bean public Job standaloneEclJob() { return wrapStep("standaloneEclJob", eclManagerStep); }
    @Bean public Job standaloneAllowanceEclCompletionJob() { return wrapStep("standaloneAllowanceEclCompletionJob", allowanceEclCompletionStep); }
    @Bean public Job standaloneAllowanceSummaryJob() { return wrapStep("standaloneAllowanceSummaryJob", allowanceSummaryStep); }

    @Bean public Job standaloneCdmSyncJob() { return wrapStep("standaloneCdmSyncJob", cdmSyncStep); }
    @Bean public Job standaloneAllowanceExposureSyncJob() { return wrapStep("standaloneAllowanceExposureSyncJob", allowanceExposureSyncStep); }

    private Job wrapStep(String jobName, Step step) {
        return new JobBuilder(jobName, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step)
                .build();
    }
}
