package com.risk.credit.batch.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;

/**
 * 🏃 [Job Runner] 신용리스크 전용 배치 실행 제어기
 * 
 * 💡 [초보자를 위한 가이드]
 * 이 클래스는 신용리스크 배치를 실제로 '실행'시키는 방아쇠 역할을 합니다.
 * `interest-batch` 서비스와 동일한 아키텍처 패턴을 적용하여, 프로그램 실행 시 
 * 커맨드라인에서 기준일(`baseDate=...`) 등의 파라미터를 받아 배치를 제어할 수 있게 합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobRunner implements CommandLineRunner {

    private final JobLauncher jobLauncher;
    private final Map<String, Job> jobs;

    @Value("${spring.batch.job.enabled:true}")
    private boolean batchJobEnabled;

    @Override
    public void run(String... args) throws Exception {
        if (!batchJobEnabled) {
            log.info(">> [Credit Risk Batch] spring.batch.job.enabled=false, CommandLineRunner 실행을 건너뜁니다.");
            return;
        }

        log.info(">> [Credit Risk Batch] CommandLineRunner 시작...");

        // 커맨드라인 인자에서 baseDate 추출 (없으면 오늘 날짜)
        String baseDateStr = Arrays.stream(args)
                .filter(arg -> arg.startsWith("baseDate="))
                .map(arg -> arg.substring("baseDate=".length()))
                .findFirst()
                .orElse(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        String jobName = Arrays.stream(args)
                .filter(arg -> arg.startsWith("job.name=") || arg.startsWith("spring.batch.job.name="))
                .map(arg -> arg.substring(arg.indexOf('=') + 1))
                .findFirst()
                .orElse("creditRiskMasterJob");

        Job targetJob = jobs.get(jobName);
        if (targetJob == null) {
            throw new IllegalArgumentException("Unknown credit risk batch job: " + jobName);
        }

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDate", baseDateStr)
                .addString("runId", String.valueOf(System.currentTimeMillis())) // 중복 실행 방지 및 인스턴스 고유화
                .toJobParameters();

        log.info(">> [신용리스크 배치] 실행 시도 (Job: {}, 기준일: {})", jobName, baseDateStr);
        jobLauncher.run(targetJob, jobParameters);
    }
}
