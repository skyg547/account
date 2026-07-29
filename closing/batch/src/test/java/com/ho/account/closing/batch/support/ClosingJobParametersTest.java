package com.ho.account.closing.batch.support;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClosingJobParametersTest {

    @Test
    void validatorRejectsMissingDate() {
        JobParameters parameters = new JobParameters(Map.of(
                "valuationBatchId", new JobParameter<>(77L, Long.class)));

        assertThatThrownBy(() -> ClosingJobParameters
                .requiredDateAndPositiveId("valuationDate", "valuationBatchId")
                .validate(parameters))
                .hasMessageContaining("valuationDate is required");
    }

    @Test
    void validatorRejectsNonPositiveBatchId() {
        JobParameters parameters = new JobParameters(Map.of(
                "valuationDate", new JobParameter<>("2026-05-31", String.class),
                "valuationBatchId", new JobParameter<>(0L, Long.class)));

        assertThatThrownBy(() -> ClosingJobParameters
                .requiredDateAndPositiveId("valuationDate", "valuationBatchId")
                .validate(parameters))
                .hasMessageContaining("positive long");
    }

    @Test
    void validatorRejectsInvalidDate() {
        JobParameters parameters = new JobParameters(Map.of(
                "valuationDate", new JobParameter<>("2026-02-30", String.class),
                "valuationBatchId", new JobParameter<>(77L, Long.class)));

        assertThatThrownBy(() -> ClosingJobParameters
                .requiredDateAndPositiveId("valuationDate", "valuationBatchId")
                .validate(parameters))
                .hasMessageContaining("ISO date");
    }
}
