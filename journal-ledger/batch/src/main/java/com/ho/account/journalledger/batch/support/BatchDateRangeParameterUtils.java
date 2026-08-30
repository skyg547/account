package com.ho.account.journalledger.batch.support;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * journal-ledger Batch JobParameter를 업무 기준 기간으로 변환한다.
 *
 * <p>초보자 설명: batch 모듈은 Spring Batch의 StepExecution을 알아도 되지만,
 * core의 LedgerService는 순수하게 LocalDate 기간만 받는다. 이 클래스가 그 경계를 변환한다.
 */
public final class BatchDateRangeParameterUtils {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private BatchDateRangeParameterUtils() {
    }

    public static DateRange resolveDateRange(StepExecution stepExecution) {
        if (stepExecution == null) {
            throw new IllegalArgumentException("stepExecution must not be null");
        }
        return resolveDateRange(stepExecution.getJobParameters());
    }

    public static DateRange resolveDateRange(JobParameters jobParameters) {
        if (jobParameters == null) {
            throw new IllegalArgumentException("jobParameters must not be null");
        }
        LocalDate fallback = LocalDate.now().minusDays(1);
        LocalDate resolvedStart = findFirstNonBlank(
                jobParameters.getString("startDate"),
                jobParameters.getString("fromDate"),
                jobParameters.getString("baseDate"),
                jobParameters.getString("targetDate")
        ).map(date -> LocalDate.parse(date, DATE_FORMAT)).orElse(fallback);

        LocalDate resolvedEnd = findFirstNonBlank(
                jobParameters.getString("endDate"),
                jobParameters.getString("toDate"),
                jobParameters.getString("baseDate"),
                jobParameters.getString("targetDate")
        ).map(date -> LocalDate.parse(date, DATE_FORMAT)).orElse(resolvedStart);

        if (resolvedEnd.isBefore(resolvedStart)) {
            throw new IllegalArgumentException("endDate must be greater than or equal to startDate");
        }
        return new DateRange(resolvedStart, resolvedEnd);
    }

    private static Optional<String> findFirstNonBlank(String... values) {
        if (values == null) {
            return Optional.empty();
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return Optional.of(value.trim());
            }
        }
        return Optional.empty();
    }

    public record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}