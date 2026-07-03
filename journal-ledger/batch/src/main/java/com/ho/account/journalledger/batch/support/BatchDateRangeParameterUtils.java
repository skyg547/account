package com.ho.account.journalledger.batch.support;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
        return resolveDateRange(stepExecution.getJobParameters());
    }

    public static DateRange resolveDateRange(JobParameters jobParameters) {
        String startDate = firstNonBlank(
                jobParameters.getString("startDate"),
                jobParameters.getString("fromDate"),
                jobParameters.getString("baseDate"),
                jobParameters.getString("targetDate")
        );
        String endDate = firstNonBlank(
                jobParameters.getString("endDate"),
                jobParameters.getString("toDate"),
                jobParameters.getString("baseDate"),
                jobParameters.getString("targetDate")
        );

        LocalDate fallback = LocalDate.now().minusDays(1);
        LocalDate resolvedStart = startDate == null ? fallback : LocalDate.parse(startDate, DATE_FORMAT);
        LocalDate resolvedEnd = endDate == null ? resolvedStart : LocalDate.parse(endDate, DATE_FORMAT);
        if (resolvedEnd.isBefore(resolvedStart)) {
            throw new IllegalArgumentException("endDate must be greater than or equal to startDate");
        }
        return new DateRange(resolvedStart, resolvedEnd);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    public record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}