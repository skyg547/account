package com.ho.account.mart.batch.support;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MartBatchJobRunnerTest {
    private static final String JOB_NAME = "integratedPositionEtlJob";

    private ApplicationContextRunner context(Job job, JobLauncher launcher) {
        return new ApplicationContextRunner()
                .withUserConfiguration(MartBatchJobRunner.class)
                .withBean(JOB_NAME, Job.class, () -> job)
                .withBean(JobLauncher.class, () -> launcher);
    }

    @Test
    void bootEnabledHasOneRunnerAndPreservesExactTypedIdentity() throws Exception {
        Job job = mock(Job.class);
        JobLauncher launcher = mock(JobLauncher.class);
        when(job.getName()).thenReturn(JOB_NAME);
        when(job.getJobParametersIncrementer()).thenReturn(new RunIdIncrementer());
        when(launcher.run(eq(job), any(JobParameters.class))).thenReturn(new JobExecution(1L));
        JobLauncherApplicationRunner boot = new JobLauncherApplicationRunner(
                launcher, mock(JobExplorer.class), mock(JobRepository.class));
        boot.setJobs(java.util.List.of(job));
        boot.setJobName(JOB_NAME);

        // Supply the real Boot runner that auto-configuration enables for this property.
        // The context exercises the custom component's condition; the invocation below
        // exercises Boot's actual converter/incrementer merge without a database.
        context(job, launcher)
                .withPropertyValues("spring.batch.job.name=" + JOB_NAME, "spring.batch.job.enabled=true")
                .withBean(JobLauncherApplicationRunner.class, () -> boot)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(MartBatchJobRunner.class);
                    assertThat(context.getBeansOfType(ApplicationRunner.class)).hasSize(1);
                    context.getBean(ApplicationRunner.class).run(new DefaultApplicationArguments(
                            "--spring.batch.job.name=" + JOB_NAME,
                            "baseDate=2090-01-15", "run.id=6900001,java.lang.Long"));
                });

        ArgumentCaptor<JobParameters> captured = ArgumentCaptor.forClass(JobParameters.class);
        verify(launcher).run(eq(job), captured.capture());
        JobParameters parameters = captured.getValue();
        assertThat(parameters.getParameters()).containsOnlyKeys("baseDate", "run.id");
        assertThat(parameters.getString("baseDate")).isEqualTo("2090-01-15");
        assertThat(parameters.getLong("run.id")).isEqualTo(6900001L);
        assertThat(parameters.getIdentifyingParameters()).hasSize(2);
    }

    @Test
    void explicitlyDisabledBootPreservesLegacyNamedExecution() throws Exception {
        Job job = mock(Job.class);
        JobLauncher launcher = mock(JobLauncher.class);
        JobExecution completed = new JobExecution(1L);
        completed.setStatus(BatchStatus.COMPLETED);
        when(launcher.run(eq(job), any(JobParameters.class))).thenReturn(completed);
        context(job, launcher)
                .withPropertyValues("spring.batch.job.name=" + JOB_NAME, "spring.batch.job.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(MartBatchJobRunner.class);
                    context.getBean(MartBatchJobRunner.class).run(
                            new DefaultApplicationArguments("baseDate=2090-01-15"));
                });
        ArgumentCaptor<JobParameters> captured = ArgumentCaptor.forClass(JobParameters.class);
        verify(launcher).run(eq(job), captured.capture());
        assertThat(captured.getValue().getString("baseDate")).isEqualTo("2090-01-15");
        assertThat(captured.getValue().getLong("time")).isPositive();
    }

    @Test
    void missingEnablePropertyUsesBootDefaultAndMissingNameDoesNotLaunchLegacyRunner() {
        context(mock(Job.class), mock(JobLauncher.class))
                .withPropertyValues("spring.batch.job.name=" + JOB_NAME)
                .run(context -> assertThat(context).doesNotHaveBean(MartBatchJobRunner.class));
        context(mock(Job.class), mock(JobLauncher.class))
                .withPropertyValues("spring.batch.job.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MartBatchJobRunner.class));
    }
}
