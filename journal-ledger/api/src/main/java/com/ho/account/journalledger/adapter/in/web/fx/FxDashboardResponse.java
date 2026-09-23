package com.ho.account.journalledger.adapter.in.web.fx;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Read-only FX position snapshot consumed by the frontend dashboard.
 */
public record FxDashboardResponse(
        BigDecimal totalNetPosition,
        BigDecimal totalKrwAmount,
        BigDecimal dailyValuationGainLoss,
        BigDecimal gainLossPercent,
        List<FxRateDto> rates,
        List<FxPositionDto> positions
) {

    public FxDashboardResponse {
        Objects.requireNonNull(totalNetPosition, "totalNetPosition must not be null");
        Objects.requireNonNull(totalKrwAmount, "totalKrwAmount must not be null");
        Objects.requireNonNull(dailyValuationGainLoss, "dailyValuationGainLoss must not be null");
        Objects.requireNonNull(gainLossPercent, "gainLossPercent must not be null");
        rates = List.copyOf(Objects.requireNonNull(rates, "rates must not be null"));
        positions = List.copyOf(Objects.requireNonNull(positions, "positions must not be null"));

        BigDecimal positionKrwTotal = positions.stream()
                .map(FxPositionDto::krwAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalKrwAmount.compareTo(positionKrwTotal) != 0) {
            throw new IllegalArgumentException("totalKrwAmount must equal the sum of positions.krwAmount");
        }

        BigDecimal positionGainLossTotal = positions.stream()
                .map(FxPositionDto::valuationGainLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (dailyValuationGainLoss.compareTo(positionGainLossTotal) != 0) {
            throw new IllegalArgumentException(
                    "dailyValuationGainLoss must equal the sum of positions.valuationGainLoss");
        }
    }

    public record FxRateDto(
            String pair,
            BigDecimal rate,
            BigDecimal changeAmount,
            BigDecimal changePercent
    ) {

        public FxRateDto {
            Objects.requireNonNull(pair, "pair must not be null");
            Objects.requireNonNull(rate, "rate must not be null");
            Objects.requireNonNull(changeAmount, "changeAmount must not be null");
            Objects.requireNonNull(changePercent, "changePercent must not be null");
        }
    }

    public record FxPositionDto(
            String currencyCode,
            String currencyName,
            BigDecimal foreignAmount,
            BigDecimal averageRate,
            BigDecimal krwAmount,
            BigDecimal valuationGainLoss,
            String limitStatus
    ) {

        private static final Set<String> LIMIT_STATUSES = Set.of("SAFE", "WARNING", "EXCEEDED");

        public FxPositionDto {
            Objects.requireNonNull(currencyCode, "currencyCode must not be null");
            Objects.requireNonNull(currencyName, "currencyName must not be null");
            Objects.requireNonNull(foreignAmount, "foreignAmount must not be null");
            Objects.requireNonNull(averageRate, "averageRate must not be null");
            Objects.requireNonNull(krwAmount, "krwAmount must not be null");
            Objects.requireNonNull(valuationGainLoss, "valuationGainLoss must not be null");
            Objects.requireNonNull(limitStatus, "limitStatus must not be null");
            if (!LIMIT_STATUSES.contains(limitStatus)) {
                throw new IllegalArgumentException("Unsupported limitStatus: " + limitStatus);
            }
        }
    }
}
