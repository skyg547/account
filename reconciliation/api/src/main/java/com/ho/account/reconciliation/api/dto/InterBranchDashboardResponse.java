package com.ho.account.reconciliation.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** Read-only inter-branch banking dashboard snapshot. */
public record InterBranchDashboardResponse(
        int unmatchedCount,
        BigDecimal totalDiscrepancyAmount,
        BigDecimal autoMatchRate,
        int unexplainedDepositsCount,
        List<InterBranchTransactionResponse> transactions) {

    public InterBranchDashboardResponse {
        Objects.requireNonNull(totalDiscrepancyAmount, "totalDiscrepancyAmount must not be null");
        Objects.requireNonNull(autoMatchRate, "autoMatchRate must not be null");
        transactions = List.copyOf(Objects.requireNonNull(transactions, "transactions must not be null"));
    }
}
