package com.ho.account.reporting.adapter.in.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.batch.repeat.RepeatStatus;

class ReportingStatementBatchConfigTest {

    @Test
    void parseBaseDate_acceptsIsoLocalDateOnly() {
        assertThat(ReportingStatementBatchConfig.parseBaseDate("2026-03-31"))
                .isEqualTo(LocalDate.of(2026, 3, 31));

        assertThatThrownBy(() -> ReportingStatementBatchConfig.parseBaseDate("2026/03/31"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("yyyy-MM-dd");
    }

    @Test
    void tasklet_passesJobParametersToBatchAdapter() throws Exception {
        ReportingBatchAdapter adapter = org.mockito.Mockito.mock(ReportingBatchAdapter.class);
        ReportingStatementBatchConfig config = new ReportingStatementBatchConfig(adapter);

        RepeatStatus status = config.reportingStatementGenerationTasklet("2026-03-31", "tester")
                .execute(null, null);

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(adapter).runStatementGenerationBatch(LocalDate.of(2026, 3, 31), "tester");
    }
}
