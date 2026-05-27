package com.risk.credit.core.domain.allowance;

import java.time.LocalDate;
import java.util.Objects;

public record AllowanceSummaryBuildResult(
        LocalDate baseDate,
        String runId,
        String modelVersion,
        int sourceResultCount,
        int summaryRowCount) {

    public AllowanceSummaryBuildResult {
        Objects.requireNonNull(baseDate, "baseDate must not be null");
        if (isBlank(runId)) {
            throw new IllegalArgumentException("runId must not be blank");
        }
        if (isBlank(modelVersion)) {
            throw new IllegalArgumentException("modelVersion must not be blank");
        }
        if (sourceResultCount < 0) {
            throw new IllegalArgumentException("sourceResultCount must not be negative");
        }
        if (summaryRowCount < 0) {
            throw new IllegalArgumentException("summaryRowCount must not be negative");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
