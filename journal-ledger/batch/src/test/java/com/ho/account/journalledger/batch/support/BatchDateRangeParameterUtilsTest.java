package com.ho.account.journalledger.batch.support;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepExecution;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BatchDateRangeParameterUtilsTest {

    @Test
    void resolvesExplicitStartAndEndDate() {
        var parameters = new JobParametersBuilder()
                .addString("startDate", "2026-04-01")
                .addString("endDate", "2026-04-30")
                .toJobParameters();

        BatchDateRangeParameterUtils.DateRange range = BatchDateRangeParameterUtils.resolveDateRange(parameters);

        assertThat(range.startDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(range.endDate()).isEqualTo(LocalDate.of(2026, 4, 30));
    }

    @Test
    void resolvesSingleBaseDateAsOneDayRange() {
        var parameters = new JobParametersBuilder()
                .addString("baseDate", "2026-04-30")
                .toJobParameters();

        BatchDateRangeParameterUtils.DateRange range = BatchDateRangeParameterUtils.resolveDateRange(parameters);

        assertThat(range.startDate()).isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(range.endDate()).isEqualTo(LocalDate.of(2026, 4, 30));
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        var parameters = new JobParametersBuilder()
                .addString("startDate", "2026-04-30")
                .addString("endDate", "2026-04-01")
                .toJobParameters();

        assertThatThrownBy(() -> BatchDateRangeParameterUtils.resolveDateRange(parameters))
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
}
