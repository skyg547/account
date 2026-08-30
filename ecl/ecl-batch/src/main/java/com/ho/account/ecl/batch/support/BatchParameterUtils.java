package com.ho.account.ecl.batch.support;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public final class BatchParameterUtils {

    private static final DateTimeFormatter DEFAULT_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private BatchParameterUtils() {
    }

    public static LocalDate resolveBaseDate(StepExecution stepExecution) {
        if (stepExecution == null) {
            throw new IllegalArgumentException("stepExecution must not be null");
        }
        return resolveBaseDate(stepExecution.getJobParameters());
    }

    public static LocalDate resolveBaseDate(JobParameters jobParameters) {
        if (jobParameters == null) {
            throw new IllegalArgumentException("jobParameters must not be null");
        }
        return resolveBaseDate(jobParameters.getString("baseDate"), jobParameters.getString("baseDt"));
    }

    public static LocalDate resolveBaseDate(String baseDate, String baseDt) {
        String resolved = findFirstNonBlank(baseDate, baseDt)
                .orElseThrow(() -> new IllegalArgumentException("baseDate(or baseDt) job parameter is required"));
        return LocalDate.parse(resolved, DEFAULT_DATE_FORMAT);
    }

    private static Optional<String> findFirstNonBlank(String... candidates) {
        if (candidates == null) {
            return Optional.empty();
        }
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return Optional.of(candidate.trim());
            }
        }
        return Optional.empty();
    }
}