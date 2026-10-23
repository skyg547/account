package com.ho.account.journalledger.batch.support;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
    private static final String[] START_DATE_ALIASES = {"startDate", "fromDate"};
    private static final String[] END_DATE_ALIASES = {"endDate", "toDate"};
    private static final String[] SINGLE_DATE_ALIASES = {"baseDate", "targetDate"};

    private BatchDateRangeParameterUtils() {
    }

    public static DateRange resolveDateRange(StepExecution stepExecution) {
        return resolveDateRange(stepExecution, Clock.systemDefaultZone());
    }

    public static DateRange resolveDateRange(StepExecution stepExecution, Clock clock) {
        if (stepExecution == null) {
            throw new IllegalArgumentException("stepExecution must not be null");
        }
        return resolveDateRange(stepExecution.getJobParameters(), clock);
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
        return resolveDateRange(jobParameters, Clock.systemDefaultZone());
    }

    /**
     * Resolves one unambiguous requested range before the owner barrier is acquired.
     *
     * <p>Equal duplicate aliases are accepted for launcher compatibility. A start-only range remains
     * a one-day range; an end-only range starts on the prior calendar day in the supplied clock's
     * zone. Conflicting aliases and mixing a single-day alias with range aliases are rejected before
     * cleanup. With no date parameter, that same prior calendar day is used for both bounds.</p>
     */
    public static DateRange resolveDateRange(JobParameters jobParameters, Clock clock) {
        if (jobParameters == null) {
            throw new IllegalArgumentException("jobParameters must not be null");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }

        Optional<LocalDate> start = resolveAliases(jobParameters, "start date", START_DATE_ALIASES);
        Optional<LocalDate> end = resolveAliases(jobParameters, "end date", END_DATE_ALIASES);
        Optional<LocalDate> single = resolveAliases(jobParameters, "single date", SINGLE_DATE_ALIASES);
        boolean hasRangeAlias = start.isPresent() || end.isPresent();

        if (single.isPresent() && hasRangeAlias) {
            throw new IllegalArgumentException(
                    "Single-date aliases (baseDate/targetDate) cannot be mixed with range aliases");
        }
        if (single.isPresent()) {
            return new DateRange(single.get(), single.get());
        }
        if (!hasRangeAlias) {
            LocalDate yesterday = LocalDate.now(clock).minusDays(1);
            return new DateRange(yesterday, yesterday);
        }
        LocalDate resolvedStart = start.orElseGet(() -> LocalDate.now(clock).minusDays(1));
        LocalDate resolvedEnd = end.orElse(resolvedStart);
        if (resolvedEnd.isBefore(resolvedStart)) {
            throw new IllegalArgumentException("endDate must be greater than or equal to startDate");
        }
        return new DateRange(resolvedStart, resolvedEnd);
    }

    private static Optional<LocalDate> resolveAliases(
            JobParameters jobParameters, String semanticName, String... aliases) {
        LocalDate resolved = null;
        String resolvedAlias = null;
        for (String alias : aliases) {
            String rawValue = jobParameters.getString(alias);
            if (rawValue == null || rawValue.isBlank()) {
                continue;
            }
            LocalDate candidate;
            try {
                candidate = LocalDate.parse(rawValue.trim(), DATE_FORMAT);
            } catch (DateTimeParseException exception) {
                throw new IllegalArgumentException(
                        "Invalid " + alias + "; expected an ISO date such as 2026-04-30", exception);
            }
            if (resolved != null && !resolved.equals(candidate)) {
                throw new IllegalArgumentException(
                        "Conflicting " + semanticName + " aliases: " + resolvedAlias + " and " + alias);
            }
            resolved = candidate;
            resolvedAlias = alias;
        }
        return Optional.ofNullable(resolved);
    }

    public record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}
