package com.ho.account.cashflow.core.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public final class CashflowForecast {

    private final String forecastId;
    private final LocalDate forecastDate;
    private final LocalDate targetDate;
    private final BigDecimal inflowEstimate;
    private final BigDecimal outflowEstimate;
    private final BigDecimal netLiquidity;
    private final CashflowRiskLevel riskLevel;
    private final String notes;

    private CashflowForecast(
            String forecastId,
            LocalDate forecastDate,
            LocalDate targetDate,
            BigDecimal inflowEstimate,
            BigDecimal outflowEstimate,
            BigDecimal netLiquidity,
            CashflowRiskLevel riskLevel,
            String notes) {
        this.forecastId = requireText(forecastId, "forecastId");
        this.forecastDate = Objects.requireNonNull(forecastDate, "forecastDate must not be null");
        this.targetDate = Objects.requireNonNull(targetDate, "targetDate must not be null");
        if (targetDate.isBefore(forecastDate)) {
            throw new IllegalArgumentException("targetDate must not be before forecastDate");
        }
        this.inflowEstimate = requireNonNegative(inflowEstimate, "inflowEstimate");
        this.outflowEstimate = requireNonNegative(outflowEstimate, "outflowEstimate");
        this.netLiquidity = CashflowAmounts.normalize(netLiquidity, "netLiquidity");
        this.riskLevel = Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        this.notes = notes == null ? "" : notes.trim();
        if (this.netLiquidity.compareTo(this.inflowEstimate.subtract(this.outflowEstimate)) != 0) {
            throw new IllegalArgumentException("netLiquidity must equal inflowEstimate - outflowEstimate");
        }
    }

    public static CashflowForecast create(
            String forecastId,
            LocalDate forecastDate,
            LocalDate targetDate,
            BigDecimal inflowEstimate,
            BigDecimal outflowEstimate,
            BigDecimal watchThreshold,
            BigDecimal criticalThreshold,
            String notes) {
        BigDecimal inflow = requireNonNegative(inflowEstimate, "inflowEstimate");
        BigDecimal outflow = requireNonNegative(outflowEstimate, "outflowEstimate");
        BigDecimal watch = CashflowAmounts.normalize(watchThreshold, "watchThreshold");
        BigDecimal critical = CashflowAmounts.normalize(criticalThreshold, "criticalThreshold");
        if (critical.compareTo(watch) > 0) {
            throw new IllegalArgumentException("criticalThreshold must be less than or equal to watchThreshold");
        }
        BigDecimal net = inflow.subtract(outflow).setScale(CashflowAmounts.SCALE);
        CashflowRiskLevel risk = assessRisk(net, watch, critical);
        return new CashflowForecast(
                forecastId, forecastDate, targetDate, inflow, outflow, net, risk, notes);
    }

    private static CashflowRiskLevel assessRisk(
            BigDecimal netLiquidity, BigDecimal watchThreshold, BigDecimal criticalThreshold) {
        // Boundary values belong to the less severe band so an alert escalates only after crossing a threshold.
        if (netLiquidity.compareTo(watchThreshold) >= 0) {
            return CashflowRiskLevel.NORMAL;
        }
        if (netLiquidity.compareTo(criticalThreshold) >= 0) {
            return CashflowRiskLevel.WATCH;
        }
        return CashflowRiskLevel.CRITICAL;
    }

    private static BigDecimal requireNonNegative(BigDecimal amount, String fieldName) {
        BigDecimal normalized = CashflowAmounts.normalize(amount, fieldName);
        if (normalized.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return normalized;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    public String getForecastId() { return forecastId; }
    public LocalDate getForecastDate() { return forecastDate; }
    public LocalDate getTargetDate() { return targetDate; }
    public BigDecimal getInflowEstimate() { return inflowEstimate; }
    public BigDecimal getOutflowEstimate() { return outflowEstimate; }
    public BigDecimal getNetLiquidity() { return netLiquidity; }
    public CashflowRiskLevel getRiskLevel() { return riskLevel; }
    public String getNotes() { return notes; }
}
