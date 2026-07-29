package com.ho.account.closing.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
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
        runId = requireText(runId, "runId");
        modelVersion = requireText(modelVersion, "modelVersion");
        legalEntityCode = requireText(legalEntityCode, "legalEntityCode");
        currencyCode = requireText(currencyCode, "currencyCode").toUpperCase(Locale.ROOT);
        if (!currencyCode.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("currencyCode must be a 3-letter currency code");
        }
        targetAllowanceAmount = requireNonNegative(targetAllowanceAmount, "targetAllowanceAmount");
        sourceExposureAmount = requireNonNegative(sourceExposureAmount, "sourceExposureAmount");
        stage1AllowanceAmount = requireNonNegative(stage1AllowanceAmount, "stage1AllowanceAmount");
        stage2AllowanceAmount = requireNonNegative(stage2AllowanceAmount, "stage2AllowanceAmount");
        stage3AllowanceAmount = requireNonNegative(stage3AllowanceAmount, "stage3AllowanceAmount");
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
