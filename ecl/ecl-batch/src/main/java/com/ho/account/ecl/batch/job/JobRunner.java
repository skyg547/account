package com.ho.account.ecl.batch.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;

/**
 * 🏃 [Job Runner] 대손충당금(IFRS9) 전용 배치 실행 제어기
 * 
 * 💡 [초보자를 위한 가이드]
 * 이 클래스는 대손충당금(IFRS9) 배치를 실제로 '실행'시키는 방아쇠 역할을 합니다.
 * `interest-batch` 서비스와 동일한 아키텍처 패턴을 적용하여, 프로그램 실행 시 
 * 커맨드라인에서 기준일(`baseDate=...`) 등의 파라미터를 받아 배치를 제어할 수 있게 합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobRunner implements CommandLineRunner {

    private final JobLauncher jobLauncher;
    private final Map<String, Job> jobs;

    @Override
    public void run(String... args) throws Exception {
        String jobName = findArgumentValue(args, "job.name", "spring.batch.job.name");
        if (!hasText(jobName)) {
            log.info(">> [IFRS9 Allowance Batch] job.name이 없어 CommandLineRunner 실행을 건너뜁니다.");
            return;
        }

        log.info(">> [IFRS9 Allowance Batch] CommandLineRunner 시작...");

        // 커맨드라인 인자에서 baseDate 추출 (없으면 오늘 날짜)
        String baseDateStr = findArgumentValue(args, "baseDate");
        if (!hasText(baseDateStr)) {
            baseDateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }

        String runId = findArgumentValue(args, "runId");
        String modelVersion = findArgumentValue(args, "modelVersion");

        Job targetJob = jobs.get(jobName);
        if (targetJob == null) {
            throw new IllegalArgumentException("Unknown IFRS9 allowance batch job: " + jobName);
        }

        JobParametersBuilder jobParametersBuilder = new JobParametersBuilder()
                .addString("baseDate", baseDateStr)
                .addString("runId", hasText(runId) ? runId.trim() : String.valueOf(System.currentTimeMillis()));
        if (hasText(modelVersion)) {
            jobParametersBuilder.addString("modelVersion", modelVersion.trim());
        }

        log.info(">> [대손충당금(IFRS9) 배치] 실행 시도 (Job: {}, 기준일: {})", jobName, baseDateStr);
        JobExecution execution = jobLauncher.run(targetJob, jobParametersBuilder.toJobParameters());
        if (!BatchStatus.COMPLETED.equals(execution.getStatus())) {
            throw new IllegalStateException("IFRS9 allowance batch job failed: " + execution.getExitStatus());
        }
    }

    private static String findArgumentValue(String[] args, String... keys) {
        return Arrays.stream(args)
                .map(JobRunner::stripOptionPrefix)
                .filter(arg -> Arrays.stream(keys).anyMatch(key -> arg.startsWith(key + "=")))
                .map(arg -> arg.substring(arg.indexOf('=') + 1))
                .findFirst()
                .orElse(null);
    }

    private static String stripOptionPrefix(String arg) {
        if (arg == null) {
            return "";
        }
        return arg.startsWith("--") ? arg.substring(2) : arg;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
