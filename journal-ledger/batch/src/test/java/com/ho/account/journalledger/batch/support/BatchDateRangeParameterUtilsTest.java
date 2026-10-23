package com.ho.account.journalledger.batch.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepExecution;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BatchDateRangeParameterUtilsTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void resolvesYesterdayFromTheInjectedClockWhenNoDateWasSupplied() {
        var parameters = new JobParametersBuilder().toJobParameters();

        BatchDateRangeParameterUtils.DateRange range =
                BatchDateRangeParameterUtils.resolveDateRange(parameters, FIXED_CLOCK);

        assertThat(range).isEqualTo(new BatchDateRangeParameterUtils.DateRange(
                LocalDate.of(2026, 4, 30), LocalDate.of(2026, 4, 30)));
    }

    @Test
    void resolvesExplicitStartAndEndDate() {
        var parameters = new JobParametersBuilder()
                .addString("startDate", "2026-04-01")
                .addString("endDate", "2026-04-30")
                .toJobParameters();

        BatchDateRangeParameterUtils.DateRange range =
                BatchDateRangeParameterUtils.resolveDateRange(parameters, FIXED_CLOCK);

        assertThat(range.startDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(range.endDate()).isEqualTo(LocalDate.of(2026, 4, 30));
    }

    @Test
    void resolvesSingleBaseDateAsOneDayRange() {
        var parameters = new JobParametersBuilder()
                .addString("baseDate", "2026-04-30")
                .toJobParameters();

        BatchDateRangeParameterUtils.DateRange range =
                BatchDateRangeParameterUtils.resolveDateRange(parameters, FIXED_CLOCK);

        assertThat(range.startDate()).isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(range.endDate()).isEqualTo(LocalDate.of(2026, 4, 30));
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        var parameters = new JobParametersBuilder()
                .addString("startDate", "2026-04-30")
                .addString("endDate", "2026-04-01")
                .toJobParameters();

        assertThatThrownBy(() -> BatchDateRangeParameterUtils.resolveDateRange(parameters, FIXED_CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endDate");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidDateParameters")
    void rejectsMalformedAndAmbiguousDateParameters(
            String description, JobParametersBuilder parameters, String expectedMessage) {
        assertThatThrownBy(() -> BatchDateRangeParameterUtils.resolveDateRange(
                parameters.toJobParameters(), FIXED_CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedMessage);
    }

    @Test
    void acceptsSameValueDuplicateRangeAliases() {
        var parameters = new JobParametersBuilder()
                .addString("startDate", "2026-04-01")
                .addString("fromDate", " 2026-04-01 ")
                .addString("endDate", "2026-04-30")
                .addString("toDate", "2026-04-30")
                .toJobParameters();

        assertThat(BatchDateRangeParameterUtils.resolveDateRange(parameters, FIXED_CLOCK))
                .isEqualTo(new BatchDateRangeParameterUtils.DateRange(
                        LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)));
    }

    @Test
    void acceptsSameValueDuplicateSingleDateAliases() {
        var parameters = new JobParametersBuilder()
                .addString("baseDate", "2026-04-30")
                .addString("targetDate", " 2026-04-30 ")
                .toJobParameters();

        assertThat(BatchDateRangeParameterUtils.resolveDateRange(parameters, FIXED_CLOCK))
                .isEqualTo(new BatchDateRangeParameterUtils.DateRange(
                        LocalDate.of(2026, 4, 30), LocalDate.of(2026, 4, 30)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("compatiblePartialRanges")
    void preservesCompatiblePartialRangeSemantics(
            String description, JobParametersBuilder parameters,
            LocalDate expectedStart, LocalDate expectedEnd) {
        assertThat(BatchDateRangeParameterUtils.resolveDateRange(
                parameters.toJobParameters(), FIXED_CLOCK))
                .isEqualTo(new BatchDateRangeParameterUtils.DateRange(expectedStart, expectedEnd));
    }

    @Test
    void rejectsEndOnlyDateBeforeTheFixedClockYesterday() {
        var parameters = new JobParametersBuilder()
                .addString("endDate", "2026-04-29")
                .toJobParameters();

        assertThatThrownBy(() -> BatchDateRangeParameterUtils.resolveDateRange(parameters, FIXED_CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endDate");
    }

    @Test
    void freezesNormalizedAliasesInJobExecutionContextForEveryLaterStep() {
        var parameters = new JobParametersBuilder()
                .addString("fromDate", " 2026-05-01 ")
                .addString("toDate", "2026-05-31")
                .toJobParameters();
        JobExecution execution = new JobExecution(new JobInstance(767L, "reaggregation"), parameters);
        StepExecution start = new StepExecution("start", execution);

        BatchDateRangeParameterUtils.DateRange frozen = BatchDateRangeParameterUtils.freezeDateRange(start);
        BatchDateRangeParameterUtils.DateRange later = BatchDateRangeParameterUtils.resolveFrozenDateRange(
                new StepExecution("writer", execution));

        assertThat(frozen).isEqualTo(new BatchDateRangeParameterUtils.DateRange(
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)));
        assertThat(later).isEqualTo(frozen);
        assertThat(BatchDateRangeParameterUtils.ownerJobInstanceId(start)).isEqualTo(767L);
    }

    @Test
    void freezesTheEffectiveExpandedRangeInsteadOfReResolvingRequestedParameters() {
        var parameters = new JobParametersBuilder()
                .addString("startDate", "2026-12-31")
                .addString("endDate", "2026-12-31")
                .toJobParameters();
        JobExecution execution = new JobExecution(new JobInstance(766L, "reaggregation"), parameters);
        StepExecution start = new StepExecution("start", execution);
        var effective = new BatchDateRangeParameterUtils.DateRange(
                LocalDate.of(2026, 12, 31), LocalDate.of(2027, 2, 10));

        BatchDateRangeParameterUtils.freezeDateRange(start, effective);

        assertThat(BatchDateRangeParameterUtils.resolveFrozenDateRange(
                new StepExecution("reader", execution))).isEqualTo(effective);
    }

    @Test
    void rejectsAnInvalidExplicitFrozenRangeWithoutChangingExecutionContext() {
        JobExecution execution = new JobExecution(new JobInstance(766L, "reaggregation"),
                new JobParametersBuilder().toJobParameters());
        StepExecution start = new StepExecution("start", execution);

        assertThatThrownBy(() -> BatchDateRangeParameterUtils.freezeDateRange(start,
                new BatchDateRangeParameterUtils.DateRange(
                        LocalDate.of(2027, 1, 2), LocalDate.of(2027, 1, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid date range");
        assertThat(execution.getExecutionContext().size()).isZero();
    }

    private static Stream<Arguments> invalidDateParameters() {
        return Stream.of(
                Arguments.of("malformed start", new JobParametersBuilder()
                        .addString("startDate", "2026-02-30")
                        .addString("endDate", "2026-03-01"), "Invalid startDate"),
                Arguments.of("malformed alias", new JobParametersBuilder()
                        .addString("fromDate", "not-a-date")
                        .addString("toDate", "2026-03-01"), "Invalid fromDate"),
                Arguments.of("conflicting start aliases", new JobParametersBuilder()
                        .addString("startDate", "2026-04-01")
                        .addString("fromDate", "2026-04-02")
                        .addString("endDate", "2026-04-30"), "Conflicting start date aliases"),
                Arguments.of("conflicting end aliases", new JobParametersBuilder()
                        .addString("startDate", "2026-04-01")
                        .addString("endDate", "2026-04-29")
                        .addString("toDate", "2026-04-30"), "Conflicting end date aliases"),
                Arguments.of("conflicting single aliases", new JobParametersBuilder()
                        .addString("baseDate", "2026-04-29")
                        .addString("targetDate", "2026-04-30"), "Conflicting single date aliases"),
                Arguments.of("single and range mixing", new JobParametersBuilder()
                        .addString("baseDate", "2026-04-30")
                        .addString("startDate", "2026-04-30")
                        .addString("endDate", "2026-04-30"), "cannot be mixed"));
    }

    private static Stream<Arguments> compatiblePartialRanges() {
        LocalDate yesterday = LocalDate.of(2026, 4, 30);
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 5, 2);
        return Stream.of(
                Arguments.of("startDate only", new JobParametersBuilder()
                        .addString("startDate", start.toString()), start, start),
                Arguments.of("fromDate only", new JobParametersBuilder()
                        .addString("fromDate", start.toString()), start, start),
                Arguments.of("endDate only", new JobParametersBuilder()
                        .addString("endDate", end.toString()), yesterday, end),
                Arguments.of("toDate only", new JobParametersBuilder()
                        .addString("toDate", end.toString()), yesterday, end));
    }
}
