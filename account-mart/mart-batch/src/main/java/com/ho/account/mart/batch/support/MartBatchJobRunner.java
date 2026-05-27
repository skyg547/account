package com.ho.account.mart.batch.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * spring.batch.job.name이 주어진 경우 명시적으로 잡을 실행한다.
 * Boot 기본 러너 대신 고유 time 파라미터를 보강해 동일 기준일 재실행을 허용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.batch.job", name = "name")
public class MartBatchJobRunner implements ApplicationRunner {

    private final Map<String, Job> jobs;
    private final JobLauncher jobLauncher;

    @org.springframework.beans.factory.annotation.Value("${spring.batch.job.name}")
    private String jobName;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Job job = jobs.get(jobName);
        if (job == null) {
            throw new IllegalStateException("Unknown batch job: " + jobName);
        }

        JobParameters jobParameters = buildJobParameters(args);
        log.info("🧭 [Batch Runner] '{}' 잡을 실행합니다. parameters={}", jobName, jobParameters);

        JobExecution execution = jobLauncher.run(job, jobParameters);
        if (!org.springframework.batch.core.BatchStatus.COMPLETED.equals(execution.getStatus())) {
            throw new IllegalStateException("Batch job failed: " + execution.getExitStatus());
        }
    }

    private JobParameters buildJobParameters(ApplicationArguments args) {
        JobParametersBuilder builder = new JobParametersBuilder();

        for (String optionName : args.getOptionNames()) {
            if (isInfrastructureOption(optionName)) {
                continue;
            }

            List<String> values = args.getOptionValues(optionName);
            if (values == null || values.isEmpty()) {
                continue;
            }

            builder.addString(optionName, values.get(values.size() - 1));
        }

        for (String arg : args.getNonOptionArgs()) {
            int separatorIndex = arg.indexOf('=');
            if (separatorIndex <= 0 || separatorIndex == arg.length() - 1) {
                continue;
            }

            String key = arg.substring(0, separatorIndex);
            String value = arg.substring(separatorIndex + 1);
            builder.addString(key, value);
        }

        builder.addLong("time", System.currentTimeMillis());
        return builder.toJobParameters();
    }

    private boolean isInfrastructureOption(String optionName) {
        return optionName.startsWith("spring.")
                || optionName.startsWith("logging.")
                || optionName.startsWith("server.")
                || optionName.startsWith("management.")
                || optionName.startsWith("eureka.");
    }
}
