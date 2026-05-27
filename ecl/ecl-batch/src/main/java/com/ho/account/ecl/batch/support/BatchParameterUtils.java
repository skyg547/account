package com.ho.account.ecl.batch.support;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class BatchParameterUtils {

    private static final DateTimeFormatter DEFAULT_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private BatchParameterUtils() {
    }

    public static LocalDate resolveBaseDate(StepExecution stepExecution) {
        return resolveBaseDate(stepExecution.getJobParameters());
    }

    public static LocalDate resolveBaseDate(JobParameters jobParameters) {
        return resolveBaseDate(jobParameters.getString("baseDate"), jobParameters.getString("baseDt"));
    }

    public static LocalDate resolveBaseDate(String baseDate, String baseDt) {
        String resolved = firstNonBlank(baseDate, baseDt);
        if (resolved == null) {
            throw new IllegalStateException("baseDate(or baseDt) job parameter is required");
        }
        return LocalDate.parse(resolved, DEFAULT_DATE_FORMAT);
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
