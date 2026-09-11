package com.ho.account.internalaudit.core.domain.evaluation;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.Builder;

@Builder(toBuilder = true)
public record OperatingEvaluation(
        String evaluationId,
        String controlId,
        String evaluatorId,
        String evaluationDate,
        Integer sampleSize,
        Integer exceptionCount,
        List<String> evidenceFilePaths,
        String result, // EFFECTIVE, INEFFECTIVE
        String remarks
) {
    private static final Set<String> VALID_RESULTS = Set.of("EFFECTIVE", "INEFFECTIVE");

    public OperatingEvaluation {
        if (result != null) {
            String normalized = result.trim().toUpperCase(Locale.ROOT);
            if (!VALID_RESULTS.contains(normalized)) {
                throw new IllegalArgumentException("Evaluation result must be EFFECTIVE or INEFFECTIVE: " + result);
            }
            result = normalized;
        }

        // Preserve independently unspecified counts; compare only known values.
        // See the count policy in internal-audit/docs/process-flow.md.
        if (sampleSize != null && sampleSize < 0) {
            throw new IllegalArgumentException("Sample size must not be negative");
        }
        if (exceptionCount != null && exceptionCount < 0) {
            throw new IllegalArgumentException("Exception count must not be negative");
        }
        if (sampleSize != null && exceptionCount != null && exceptionCount > sampleSize) {
            throw new IllegalArgumentException("Exception count must not exceed sample size");
        }
    }
}
