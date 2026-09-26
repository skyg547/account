package com.ho.account.journalledger.batch.support;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

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
    private static final String FROZEN_START = "balanceReaggregation.startDate";
    private static final String FROZEN_END = "balanceReaggregation.endDate";

    private BatchDateRangeParameterUtils() {
    }

    public static DateRange resolveDateRange(StepExecution stepExecution) {
        if (stepExecution == null) {
            throw new IllegalArgumentException("stepExecution must not be null");
        }
        return resolveDateRange(stepExecution.getJobParameters());
    }

    public static DateRange freezeDateRange(StepExecution stepExecution) {
        return freezeDateRange(stepExecution, resolveDateRange(stepExecution));
    }

    public static DateRange freezeDateRange(StepExecution stepExecution, DateRange range) {
        if (stepExecution == null) {
            throw new IllegalArgumentException("stepExecution must not be null");
        }
        if (range == null || range.startDate() == null || range.endDate() == null
                || range.endDate().isBefore(range.startDate())) {
            throw new IllegalArgumentException("A valid date range is required for freezing");
        }
        ExecutionContext context = stepExecution.getJobExecution().getExecutionContext();
        context.putString(FROZEN_START, range.startDate().format(DATE_FORMAT));
        context.putString(FROZEN_END, range.endDate().format(DATE_FORMAT));
        return range;
    }

    public static DateRange resolveFrozenDateRange(StepExecution stepExecution) {
        if (stepExecution == null) {
            throw new IllegalArgumentException("stepExecution must not be null");
        }
        ExecutionContext context = stepExecution.getJobExecution().getExecutionContext();
        if (!context.containsKey(FROZEN_START) || !context.containsKey(FROZEN_END)) {
            throw new IllegalStateException("Reaggregation date range was not frozen by the start step");
        }
        LocalDate start = LocalDate.parse(context.getString(FROZEN_START), DATE_FORMAT);
        LocalDate end = LocalDate.parse(context.getString(FROZEN_END), DATE_FORMAT);
        if (end.isBefore(start)) {
            throw new IllegalStateException("Frozen reaggregation range is invalid");
        }
        return new DateRange(start, end);
    }

    public static long ownerJobInstanceId(StepExecution stepExecution) {
        Long id = stepExecution.getJobExecution().getJobInstance().getInstanceId();
        if (id == null) {
            throw new IllegalStateException("Persisted JobInstance ID is required for balance reaggregation ownership");
        }
        return id;
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
