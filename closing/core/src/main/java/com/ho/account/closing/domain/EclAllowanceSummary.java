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
 *
 * <p>{@code targetAllowanceAmount}, {@code sourceExposureAmount} and all three stage allowance
 * amounts are denominated in {@code currencyCode}, the transaction currency. They are not
 * functional-currency GL amounts. Exact stage totals are verified and retained with the source
 * summary. Closing aggregates the target in these units before rounding for posting, then
 * separately converts the cumulative target at the closing-date rate.</p>
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
        // Compare exact source precision before any posting-scale rounding; a cent-level
        // tolerance could conceal a corrupt ECL snapshot or change a later group sum.
        if (stage1AllowanceAmount.add(stage2AllowanceAmount).add(stage3AllowanceAmount)
                .compareTo(targetAllowanceAmount) != 0) {
            throw new IllegalArgumentException("ECL stage allowance total must equal targetAllowanceAmount");
        }
        if (sourceExposureAmount.signum() == 0 && targetAllowanceAmount.signum() != 0) {
            throw new IllegalArgumentException("ECL zero source exposure requires zero target allowance");
        }
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
