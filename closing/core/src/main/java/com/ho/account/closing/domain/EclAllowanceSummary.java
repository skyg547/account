package com.ho.account.closing.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * ECL engine output consumed by closing provision processing.
 *
 * <p>Closing treats this as an external calculation result. It does not
 * recalculate PD, LGD, EAD, or allowance rates.</p>
 */
public record EclAllowanceSummary(
        LocalDate baseDate,
        String runId,
        String modelVersion,
        String legalEntityCode,
        String currencyCode,
        String exposureAccountCode,
        String allowanceAccountCode,
        String badDebtExpenseAccountCode,
        String reversalIncomeAccountCode,
        BigDecimal targetAllowanceAmount,
        BigDecimal sourceExposureAmount,
        BigDecimal stage1AllowanceAmount,
        BigDecimal stage2AllowanceAmount,
        BigDecimal stage3AllowanceAmount) {

    public EclAllowanceSummary {
        Objects.requireNonNull(baseDate, "baseDate must not be null");
        Objects.requireNonNull(currencyCode, "currencyCode must not be null");
        targetAllowanceAmount = requireNonNegative(targetAllowanceAmount, "targetAllowanceAmount");
        sourceExposureAmount = zeroIfNull(sourceExposureAmount);
        stage1AllowanceAmount = zeroIfNull(stage1AllowanceAmount);
        stage2AllowanceAmount = zeroIfNull(stage2AllowanceAmount);
        stage3AllowanceAmount = zeroIfNull(stage3AllowanceAmount);
    }

    public String slipDiscriminator(String resolvedAllowanceAccountCode) {
        return String.join("|",
                valueOrBlank(legalEntityCode),
                currencyCode,
                valueOrBlank(exposureAccountCode),
                valueOrBlank(resolvedAllowanceAccountCode),
                valueOrBlank(runId));
    }

    public String lineageSourceId(Long provisionBatchId) {
        String runPart = isBlank(runId) ? "no-run" : runId.trim();
        return provisionBatchId + "|" + runPart;
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
        BigDecimal normalized = zeroIfNull(value);
        if (normalized.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return normalized;
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String valueOrBlank(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
