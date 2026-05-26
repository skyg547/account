package com.risk.mart.core.support;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 배치 공통 Job Parameter 해석 유틸리티.
 * ODS/CDM 배치는 baseDate(or baseDt) 파라미터를 필수로 사용한다.
 */
public final class BatchParameterUtils {

    private static final DateTimeFormatter DEFAULT_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private BatchParameterUtils() {
    }

    public static LocalDate resolveBaseDate(StepExecution stepExecution) {
        return resolveBaseDate(stepExecution.getJobParameters());
    }

    public static LocalDate resolveBaseDate(JobParameters jobParameters) {
        String baseDate = firstNonBlank(
                jobParameters.getString("baseDt"),
                jobParameters.getString("baseDate"));

        if (baseDate == null) {
            throw new IllegalStateException("baseDate(or baseDt) job parameter is required");
        }

        return LocalDate.parse(baseDate, DEFAULT_DATE_FORMAT);
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }
}
