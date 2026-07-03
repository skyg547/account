package com.ho.account.journalledger.batch.support;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParametersBuilder;

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
}